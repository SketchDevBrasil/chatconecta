package com.my.chatconecta.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Limite de 5 mensagens por minuto por usuario, imposto pelas regras do banco.
 *
 * As regras do Realtime Database nao sabem contar filhos de um no, entao cada usuario
 * tem 5 vagas fixas em limites/{uid}/{a..e}. Cada mensagem exige uma vaga
 * { id: <id da mensagem>, em: <horario do servidor> } gravada na MESMA escrita atomica
 * da mensagem, e uma vaga so pode ser reusada 60s depois. O servidor recusa qualquer
 * coisa fora disso - inclusive apagar a vaga para zerar o contador.
 *
 * Esta classe apenas escolhe qual vaga usar; quem impoe o limite e o servidor. Por isso
 * ela pode errar sem risco: se a vaga escolhida ja estiver ocupada a escrita e recusada,
 * os dados sao recarregados e ha uma segunda tentativa.
 */
public final class Limite {

    public static final int POR_MINUTO = 5;

    private static final String[] VAGAS = {"a", "b", "c", "d", "e"};
    private static final long JANELA_MS = 60000L;
    /** Folga para diferenca de relogio: evita tentar uma vaga que o servidor ainda ve ocupada. */
    private static final long FOLGA_MS = 1000L;

    /** Vaga -> horario (do servidor) do envio que a ocupou. */
    private static final Map<String, Long> ocupadas = new HashMap<>();
    private static long offsetServidor;
    private static boolean carregado;

    private static DatabaseReference vagasRef;
    private static ValueEventListener vagasListener;
    private static DatabaseReference offsetRef;
    private static ValueEventListener offsetListener;
    private static String uidAtual;

    private Limite() {
    }

    /** Falha devolvida quando as 5 vagas do minuto estao ocupadas. */
    public static final class Excedido extends Exception {

        private final int segundos;

        Excedido(int segundos) {
            super("limite de " + POR_MINUTO + " mensagens por minuto atingido");
            this.segundos = segundos;
        }

        /** Quantos segundos faltam para a proxima vaga liberar (nunca menos de 1). */
        public int getSegundos() {
            return segundos;
        }
    }

    /** Acompanha as vagas do usuario logado. Idempotente: pode ser chamado a vontade. */
    public static synchronized void entrar() {
        if (!FirebaseInit.configurado()) {
            return;
        }
        String uid = Banco.meuUid();
        if (uid == null) {
            return;
        }
        if (uid.equals(uidAtual) && vagasListener != null) {
            return;
        }
        sair();
        uidAtual = uid;

        vagasRef = Banco.db().getReference("limites").child(uid);
        vagasListener = vagasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                aplicar(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        offsetRef = Banco.db().getReference(".info/serverTimeOffset");
        offsetListener = offsetRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long valor = snapshot.getValue(Long.class);
                offsetServidor = valor != null ? valor : 0L;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /** Solta os ouvintes (app no fundo ou logout). */
    public static synchronized void sair() {
        if (vagasRef != null && vagasListener != null) {
            vagasRef.removeEventListener(vagasListener);
        }
        if (offsetRef != null && offsetListener != null) {
            offsetRef.removeEventListener(offsetListener);
        }
        vagasRef = null;
        vagasListener = null;
        offsetRef = null;
        offsetListener = null;
        uidAtual = null;
        carregado = false;
        ocupadas.clear();
    }

    /**
     * Junta a vaga a escrita da mensagem e grava tudo numa unica escrita atomica.
     * Sem vaga livre devolve a Task falhando com {@link Excedido}, sem falar com o servidor.
     */
    static Task<Void> gravarComVaga(final String uid, final String mensagemId, final Map<String, Object> escrita) {
        entrar();
        String vaga = vagaLivre();
        if (vaga == null) {
            if (carregado) {
                return Tasks.forException(new Excedido(segundosParaLiberar()));
            }
            // Ainda sem os dados das vagas (app acabou de abrir): tenta e deixa o servidor decidir.
            vaga = VAGAS[0];
        }
        return gravar(uid, vaga, mensagemId, escrita).continueWithTask(primeira -> {
            if (primeira.isSuccessful()) {
                return primeira;
            }
            // A vaga pode ter sido escolhida com dados velhos: recarrega e tenta uma vez.
            return recarregar(uid).continueWithTask(ignorado -> {
                String outra = vagaLivre();
                if (outra == null) {
                    return Tasks.forException(new Excedido(segundosParaLiberar()));
                }
                return gravar(uid, outra, mensagemId, escrita);
            });
        });
    }

    private static Task<Void> gravar(String uid, String vaga, String mensagemId, Map<String, Object> escrita) {
        Map<String, Object> dadosVaga = new HashMap<>();
        dadosVaga.put("id", mensagemId);
        dadosVaga.put("em", ServerValue.TIMESTAMP);

        Map<String, Object> completa = new HashMap<>(escrita);
        completa.put("limites/" + uid + "/" + vaga, dadosVaga);
        return Banco.db().getReference().updateChildren(completa);
    }

    private static Task<Void> recarregar(String uid) {
        return Banco.db().getReference("limites").child(uid).get().continueWith(leitura -> {
            if (leitura.isSuccessful() && leitura.getResult() != null) {
                aplicar(leitura.getResult());
            }
            return null;
        });
    }

    private static synchronized void aplicar(DataSnapshot snapshot) {
        ocupadas.clear();
        for (DataSnapshot vaga : snapshot.getChildren()) {
            Long em = vaga.child("em").getValue(Long.class);
            if (vaga.getKey() != null && em != null) {
                ocupadas.put(vaga.getKey(), em);
            }
        }
        carregado = true;
    }

    /** Vaga nunca usada ou a mais antiga que ja passou do minuto; null se todas estao ocupadas. */
    @Nullable
    private static synchronized String vagaLivre() {
        long agora = agoraNoServidor();
        String escolhida = null;
        long maisAntiga = Long.MAX_VALUE;
        for (String vaga : VAGAS) {
            Long em = ocupadas.get(vaga);
            if (em == null) {
                return vaga;
            }
            if (agora - em <= JANELA_MS + FOLGA_MS) {
                continue;
            }
            if (em < maisAntiga) {
                maisAntiga = em;
                escolhida = vaga;
            }
        }
        return escolhida;
    }

    private static synchronized int segundosParaLiberar() {
        long agora = agoraNoServidor();
        long falta = Long.MAX_VALUE;
        for (String vaga : VAGAS) {
            Long em = ocupadas.get(vaga);
            if (em == null) {
                return 1;
            }
            falta = Math.min(falta, em + JANELA_MS + FOLGA_MS - agora);
        }
        if (falta == Long.MAX_VALUE) {
            return 1;
        }
        return (int) Math.max(1L, (falta + 999L) / 1000L);
    }

    private static long agoraNoServidor() {
        return System.currentTimeMillis() + offsetServidor;
    }
}

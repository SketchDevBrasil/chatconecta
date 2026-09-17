package com.my.chatconecta.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Todos os caminhos do Realtime Database ficam aqui. Estrutura:
 *
 *   usuarios/{uid}                         perfil publico (nome, recado, online, vistoEm)
 *   chatPublico/{id}                       mensagens da sala publica
 *   conversas/{chatId}/mensagens/{id}      mensagens privadas
 *   conversas/{chatId}/digitando/{uid}     indicador "digitando..."
 *   conversas/{chatId}/lidoEm/{uid}        ultima leitura de cada lado (confirmacao de leitura)
 *   usuarioConversas/{uid}/{contatoUid}    lista de conversas de cada usuario
 *   limites/{uid}/{a..e}                   5 vagas do limite de 5 mensagens por minuto
 *
 * As regras de seguranca correspondentes estao em database.rules.json.
 */
public final class Banco {

    private static final int TAMANHO_PREVIA = 140;

    private Banco() {
    }

    public static FirebaseDatabase db() {
        return FirebaseDatabase.getInstance(FirebaseInit.databaseUrl());
    }

    @Nullable
    public static String meuUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public static DatabaseReference usuarios() {
        return db().getReference("usuarios");
    }

    public static DatabaseReference usuario(String uid) {
        return usuarios().child(uid);
    }

    public static DatabaseReference chatPublico() {
        return db().getReference("chatPublico");
    }

    public static DatabaseReference conversa(String chatId) {
        return db().getReference("conversas").child(chatId);
    }

    public static DatabaseReference minhasConversas(String uid) {
        return db().getReference("usuarioConversas").child(uid);
    }

    /** Mesmo id para os dois lados da conversa: uids em ordem alfabetica separados por "_". */
    public static String chatId(String a, String b) {
        return a.compareTo(b) < 0 ? a + "_" + b : b + "_" + a;
    }

    public static Task<Void> criarPerfil(String uid, String nome) {
        Map<String, Object> perfil = new HashMap<>();
        perfil.put("nome", nome);
        perfil.put("nomeBusca", nome.toLowerCase(Locale.ROOT));
        perfil.put("recado", "");
        perfil.put("criadoEm", ServerValue.TIMESTAMP);
        perfil.put("online", true);
        perfil.put("vistoEm", ServerValue.TIMESTAMP);
        return usuario(uid).setValue(perfil);
    }

    public static Task<Void> atualizarPerfil(String uid, String nome, String recado) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("nomeBusca", nome.toLowerCase(Locale.ROOT));
        dados.put("recado", recado);
        return usuario(uid).updateChildren(dados);
    }

    /** Mensagem + vaga do limite numa unica escrita atomica (ver {@link Limite}). */
    public static Task<Void> enviarPublico(String uid, String nome, String texto) {
        String mensagemId = chatPublico().push().getKey();

        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("uid", uid);
        mensagem.put("nome", nome);
        mensagem.put("texto", texto);
        mensagem.put("enviadoEm", ServerValue.TIMESTAMP);

        Map<String, Object> escrita = new HashMap<>();
        escrita.put("chatPublico/" + mensagemId, mensagem);
        return Limite.gravarComVaga(uid, mensagemId, escrita);
    }

    /**
     * Grava a mensagem, a vaga do limite ({@link Limite}) e a lista de conversas dos dois
     * lados numa unica escrita atomica. Depois incrementa o contador de nao lidas do contato.
     */
    public static Task<Void> enviarPrivado(final String meuUid, final String contatoUid, String texto) {
        String chatId = chatId(meuUid, contatoUid);
        String mensagemId = conversa(chatId).child("mensagens").push().getKey();

        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("uid", meuUid);
        mensagem.put("texto", texto);
        mensagem.put("enviadoEm", ServerValue.TIMESTAMP);

        String previa = previa(texto);
        Map<String, Object> escrita = new HashMap<>();
        escrita.put("conversas/" + chatId + "/mensagens/" + mensagemId, mensagem);
        escrita.put("conversas/" + chatId + "/lidoEm/" + meuUid, ServerValue.TIMESTAMP);
        for (String[] lado : new String[][]{{meuUid, contatoUid}, {contatoUid, meuUid}}) {
            String base = "usuarioConversas/" + lado[0] + "/" + lado[1] + "/";
            escrita.put(base + "ultimaMensagem", previa);
            escrita.put(base + "ultimaEm", ServerValue.TIMESTAMP);
            escrita.put(base + "ultimoUid", meuUid);
        }
        escrita.put("usuarioConversas/" + meuUid + "/" + contatoUid + "/naoLidas", 0);

        return Limite.gravarComVaga(meuUid, mensagemId, escrita)
                .addOnSuccessListener(ok -> incrementarNaoLidas(contatoUid, meuUid));
    }

    /** Zera o contador e registra a leitura (o outro lado ve o check duplo). */
    public static void marcarComoLida(String meuUid, String contatoUid) {
        conversa(chatId(meuUid, contatoUid)).child("lidoEm").child(meuUid).setValue(ServerValue.TIMESTAMP);
        minhasConversas(meuUid).child(contatoUid).child("naoLidas").setValue(0);
    }

    private static void incrementarNaoLidas(String donoUid, String contatoUid) {
        minhasConversas(donoUid).child(contatoUid).child("naoLidas").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData atual) {
                Long valor = atual.getValue(Long.class);
                atual.setValue(valor == null ? 1L : valor + 1L);
                return Transaction.success(atual);
            }

            @Override
            public void onComplete(@Nullable DatabaseError erro, boolean confirmado, @Nullable DataSnapshot dados) {
            }
        });
    }

    private static String previa(String texto) {
        String limpo = texto.replace('\n', ' ');
        if (limpo.length() <= TAMANHO_PREVIA) {
            return limpo;
        }
        int fim = TAMANHO_PREVIA;
        if (Character.isHighSurrogate(limpo.charAt(fim - 1))) {
            fim--;
        }
        return limpo.substring(0, fim) + "\u2026";
    }
}

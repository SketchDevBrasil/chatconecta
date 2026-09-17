package com.my.chatconecta;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.my.chatconecta.adapter.MensagemAdapter;
import com.my.chatconecta.data.Banco;
import com.my.chatconecta.data.FirebaseInit;
import com.my.chatconecta.data.Limite;
import com.my.chatconecta.model.Mensagem;
import com.my.chatconecta.model.Usuario;
import com.my.chatconecta.ui.BotaoEnviar;
import com.my.chatconecta.ui.PerfilSheet;
import com.my.chatconecta.util.Avatar;
import com.my.chatconecta.util.Janela;
import com.my.chatconecta.util.Tempo;
import com.my.chatconecta.util.Texto;

/** Conversa privada entre dois usuarios, somente texto, em tempo real. */
public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "contato_uid";
    public static final String EXTRA_NOME = "contato_nome";

    private static final int LIMITE_MENSAGENS = 300;
    private static final long PARAR_DIGITANDO_MS = 4000;

    private String meuUid;
    private String contatoUid;
    private String contatoNome;
    private String chatId;
    @Nullable
    private Usuario contato;
    private boolean contatoDigitando;
    private boolean euDigitando;
    private boolean emPrimeiroPlano;
    private boolean carregado;
    private boolean temRecebidas;

    private RecyclerView recycler;
    private LinearLayoutManager layout;
    private MensagemAdapter adapter;
    private EditText edtMensagem;
    private TextView txtNome;
    private TextView txtStatus;
    private TextView txtAvatar;
    private View pontoOnline;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pararDigitando = this::pararDeDigitar;
    private final Runnable marcarLida = () -> Banco.marcarComoLida(meuUid, contatoUid);

    private Query mensagensQuery;
    private ChildEventListener mensagensListener;
    private DatabaseReference contatoRef;
    private ValueEventListener contatoListener;
    private DatabaseReference digitandoRef;
    private ValueEventListener digitandoListener;
    private DatabaseReference lidoRef;
    private ValueEventListener lidoListener;

    public static void abrir(Context context, String uid, String nome) {
        // Folha de perfil aberta de dentro desta mesma conversa: ja estamos no chat.
        if (context instanceof ChatActivity && uid.equals(((ChatActivity) context).contatoUid)) {
            return;
        }
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_NOME, nome);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseInit.ensure(this);
        FirebaseUser user = FirebaseInit.configurado() ? FirebaseAuth.getInstance().getCurrentUser() : null;
        contatoUid = getIntent().getStringExtra(EXTRA_UID);
        if (user == null || contatoUid == null || contatoUid.isEmpty() || contatoUid.equals(user.getUid())) {
            finish();
            return;
        }
        meuUid = user.getUid();
        contatoNome = getIntent().getStringExtra(EXTRA_NOME);
        if (contatoNome == null) contatoNome = "";
        chatId = Banco.chatId(meuUid, contatoUid);

        setContentView(R.layout.activity_chat);
        Janela.telaCheia(this, false);
        Janela.aplicarEspacos(findViewById(R.id.raizChat), findViewById(R.id.topoChat), findViewById(R.id.compositorChat));

        configurarCabecalho();
        configurarLista();
        configurarCompositor();

        observarMensagens();
        observarContato();
    }

    private void configurarCabecalho() {
        txtNome = findViewById(R.id.txtNomeChat);
        txtStatus = findViewById(R.id.txtStatusChat);
        txtAvatar = findViewById(R.id.txtAvatarChat);
        pontoOnline = findViewById(R.id.pontoOnlineChat);
        txtNome.setText(contatoNome);
        Avatar.aplicar(txtAvatar, contatoUid, contatoNome);
        ((TextView) findViewById(R.id.txtVazioChat))
                .setText(getString(R.string.chat_vazio_titulo, Avatar.primeiroNome(contatoNome)));

        findViewById(R.id.btnVoltarChat).setOnClickListener(v -> finish());
        findViewById(R.id.areaContatoChat).setOnClickListener(v -> abrirPerfilDoContato());
    }

    private void configurarLista() {
        recycler = findViewById(R.id.recyclerChat);
        layout = new LinearLayoutManager(this);
        layout.setStackFromEnd(true);
        recycler.setLayoutManager(layout);
        adapter = new MensagemAdapter(meuUid, false, null);
        recycler.setAdapter(adapter);
        recycler.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob) recycler.post(() -> rolarParaFim(false));
        });
    }

    private void configurarCompositor() {
        edtMensagem = findViewById(R.id.edtMensagem);
        View btnEnviar = findViewById(R.id.btnEnviar);
        BotaoEnviar.ligar(edtMensagem, btnEnviar);
        btnEnviar.setOnClickListener(v -> enviar());
        edtMensagem.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                aoDigitar(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    // ------------------------------------------------------------------ Realtime Database

    private void observarMensagens() {
        mensagensQuery = Banco.conversa(chatId).child("mensagens").orderByKey().limitToLast(LIMITE_MENSAGENS);
        mensagensListener = mensagensQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String anterior) {
                Mensagem m = Mensagem.de(snapshot);
                if (m.getTexto().isEmpty()) return;
                boolean minha = meuUid.equals(m.getUid());
                boolean estavaNoFim = layout.findLastVisibleItemPosition() >= adapter.getItemCount() - 2;
                adapter.adicionar(m);
                findViewById(R.id.vazioChat).setVisibility(View.GONE);
                if (!carregado || estavaNoFim || minha) {
                    rolarParaFim(carregado);
                }
                if (!minha) {
                    temRecebidas = true;
                    agendarLeitura();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String anterior) {
                adapter.adicionar(Mensagem.de(snapshot));
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String anterior) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                findViewById(R.id.progressChat).setVisibility(View.GONE);
                Toast.makeText(ChatActivity.this, R.string.erro_generico, Toast.LENGTH_SHORT).show();
            }
        });
        mensagensQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                carregado = true;
                findViewById(R.id.progressChat).setVisibility(View.GONE);
                findViewById(R.id.vazioChat).setVisibility(adapter.temMensagens() ? View.GONE : View.VISIBLE);
                rolarParaFim(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                findViewById(R.id.progressChat).setVisibility(View.GONE);
            }
        });

        // Quando o contato leu: minhas mensagens ate esse horario ganham o check duplo.
        lidoRef = Banco.conversa(chatId).child("lidoEm").child(contatoUid);
        lidoListener = lidoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long lidoEm = snapshot.getValue(Long.class);
                adapter.setLidoPeloContatoAte(lidoEm != null ? lidoEm : 0L);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void observarContato() {
        contatoRef = Banco.usuario(contatoUid);
        contatoListener = contatoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                contato = Usuario.de(snapshot);
                contatoNome = contato.getNome();
                txtNome.setText(contatoNome);
                Avatar.aplicar(txtAvatar, contatoUid, contatoNome);
                pontoOnline.setVisibility(contato.isOnline() ? View.VISIBLE : View.GONE);
                atualizarStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        digitandoRef = Banco.conversa(chatId).child("digitando").child(contatoUid);
        digitandoListener = digitandoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contatoDigitando = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                atualizarStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void atualizarStatus() {
        if (contatoDigitando) {
            txtStatus.setText(R.string.status_digitando);
            txtStatus.setTextColor(ContextCompat.getColor(this, R.color.brand));
            return;
        }
        txtStatus.setText(Tempo.status(this, contato));
        txtStatus.setTextColor(ContextCompat.getColor(this,
                contato != null && contato.isOnline() ? R.color.online : R.color.text_secondary));
    }

    // ------------------------------------------------------------------ envio e digitacao

    private void enviar() {
        String texto = Texto.de(edtMensagem);
        if (texto.isEmpty()) return;
        edtMensagem.setText("");
        pararDeDigitar();
        Banco.enviarPrivado(meuUid, contatoUid, texto)
                .addOnFailureListener(e -> falhaNoEnvio(e, texto, edtMensagem));
    }

    /** Devolve o texto ao campo (nao perde o que foi digitado) e explica o motivo. */
    private void falhaNoEnvio(Exception erro, String texto, EditText campo) {
        if (Texto.de(campo).isEmpty()) {
            campo.setText(texto);
            campo.setSelection(texto.length());
        }
        if (erro instanceof Limite.Excedido) {
            int segundos = ((Limite.Excedido) erro).getSegundos();
            Toast.makeText(this, getString(R.string.limite_envio, Limite.POR_MINUTO, segundos),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.erro_envio, Toast.LENGTH_SHORT).show();
        }
    }

    private void aoDigitar(boolean temTexto) {
        handler.removeCallbacks(pararDigitando);
        if (!temTexto) {
            pararDeDigitar();
            return;
        }
        if (!euDigitando) {
            euDigitando = true;
            DatabaseReference ref = meuDigitando();
            ref.onDisconnect().removeValue();
            ref.setValue(true);
        }
        handler.postDelayed(pararDigitando, PARAR_DIGITANDO_MS);
    }

    private void pararDeDigitar() {
        handler.removeCallbacks(pararDigitando);
        if (!euDigitando) return;
        euDigitando = false;
        meuDigitando().removeValue();
    }

    private DatabaseReference meuDigitando() {
        return Banco.conversa(chatId).child("digitando").child(meuUid);
    }

    /** Pequeno atraso para juntar varias mensagens chegando de uma vez em uma escrita so. */
    private void agendarLeitura() {
        if (!emPrimeiroPlano) return;
        handler.removeCallbacks(marcarLida);
        handler.postDelayed(marcarLida, 400);
    }

    private void rolarParaFim(boolean suave) {
        int total = adapter.getItemCount();
        if (total == 0) return;
        if (suave) {
            recycler.smoothScrollToPosition(total - 1);
        } else {
            recycler.scrollToPosition(total - 1);
        }
    }

    private void abrirPerfilDoContato() {
        PerfilSheet.mostrar(this, contatoUid, contatoNome);
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onResume() {
        super.onResume();
        emPrimeiroPlano = true;
        if (temRecebidas) agendarLeitura();
    }

    @Override
    protected void onPause() {
        emPrimeiroPlano = false;
        handler.removeCallbacks(marcarLida);
        pararDeDigitar();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (mensagensQuery != null && mensagensListener != null) mensagensQuery.removeEventListener(mensagensListener);
        if (lidoRef != null && lidoListener != null) lidoRef.removeEventListener(lidoListener);
        if (contatoRef != null && contatoListener != null) contatoRef.removeEventListener(contatoListener);
        if (digitandoRef != null && digitandoListener != null) digitandoRef.removeEventListener(digitandoListener);
        super.onDestroy();
    }
}

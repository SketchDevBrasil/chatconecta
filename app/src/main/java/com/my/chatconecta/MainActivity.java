package com.my.chatconecta;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.my.chatconecta.adapter.ConversaAdapter;
import com.my.chatconecta.adapter.MensagemAdapter;
import com.my.chatconecta.adapter.PessoaAdapter;
import com.my.chatconecta.data.Banco;
import com.my.chatconecta.data.FirebaseInit;
import com.my.chatconecta.data.Limite;
import com.my.chatconecta.data.Presenca;
import com.my.chatconecta.model.Conversa;
import com.my.chatconecta.model.Mensagem;
import com.my.chatconecta.model.Usuario;
import com.my.chatconecta.ui.BotaoEnviar;
import com.my.chatconecta.ui.PerfilSheet;
import com.my.chatconecta.util.Avatar;
import com.my.chatconecta.util.Janela;
import com.my.chatconecta.util.Texto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tela principal com tres abas: chat publico, minhas conversas e busca de pessoas. */
public class MainActivity extends AppCompatActivity
        implements MensagemAdapter.Listener, PessoaAdapter.Listener, ConversaAdapter.Listener {

    private static final String ESTADO_ABA = "aba";
    private static final int LIMITE_PUBLICO = 150;
    private static final int LIMITE_FAIXA = 30;

    private String meuUid;
    @Nullable
    private Usuario eu;
    private final Map<String, Usuario> usuarios = new HashMap<>();
    private final List<Usuario> pessoas = new ArrayList<>();
    private final List<Conversa> conversas = new ArrayList<>();
    private int abaAtual = R.id.nav_publico;

    private View topo;
    private View conteudo;
    private BottomNavigationView nav;
    private TextView txtTitulo;
    private TextView txtSubtitulo;
    private TextView txtMeuAvatar;
    private OnBackPressedCallback voltarParaPublico;

    private RecyclerView recyclerPublico;
    private LinearLayoutManager layoutPublico;
    private MensagemAdapter mensagemAdapter;
    private PessoaAdapter faixaAdapter;
    private EditText edtMensagemPublico;
    private View btnEnviarPublico;
    private boolean publicoCarregado;

    private ConversaAdapter conversaAdapter;
    private PessoaAdapter pessoaAdapter;
    private EditText edtBusca;
    private TextView vazioPessoas;

    private DatabaseReference usuariosRef;
    private ValueEventListener usuariosListener;
    private Query publicoQuery;
    private ChildEventListener publicoListener;
    private DatabaseReference conversasRef;
    private ValueEventListener conversasListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseInit.ensure(this);
        FirebaseUser user = FirebaseInit.configurado() ? FirebaseAuth.getInstance().getCurrentUser() : null;
        if (user == null) {
            startActivity(new Intent(this, SplashActivity.class));
            finish();
            return;
        }
        meuUid = user.getUid();

        setContentView(R.layout.activity_main);
        Janela.telaCheia(this, false);

        topo = findViewById(R.id.topoMain);
        conteudo = findViewById(R.id.conteudoMain);
        nav = findViewById(R.id.navInferior);
        txtTitulo = findViewById(R.id.txtTituloAba);
        txtSubtitulo = findViewById(R.id.txtSubtituloAba);
        txtMeuAvatar = findViewById(R.id.txtMeuAvatar);
        Avatar.aplicar(txtMeuAvatar, meuUid, "");
        txtMeuAvatar.setOnClickListener(v -> startActivity(new Intent(this, PerfilActivity.class)));

        configurarEspacos();
        configurarPublico();
        configurarConversas();
        configurarPessoas();
        configurarNavegacao(savedInstanceState);

        Presenca.entrar();
        garantirPerfil(user);
        observarUsuarios();
        observarPublico();
        observarConversas();
    }

    // ------------------------------------------------------------------ layout

    /** Teclado aberto: esconde a barra inferior e sobe o conteudo junto com o teclado. */
    private void configurarEspacos() {
        final View raiz = findViewById(R.id.raizMain);
        final int topoOriginal = topo.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int teclado = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            boolean tecladoAberto = teclado > barras.bottom;

            raiz.setPadding(barras.left, 0, barras.right, 0);
            topo.setPadding(topo.getPaddingLeft(), topoOriginal + barras.top, topo.getPaddingRight(), topo.getPaddingBottom());
            nav.setVisibility(tecladoAberto ? View.GONE : View.VISIBLE);
            nav.setPadding(0, 0, 0, barras.bottom);
            conteudo.setPadding(0, 0, 0, tecladoAberto ? teclado : 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void configurarNavegacao(@Nullable Bundle estado) {
        voltarParaPublico = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                nav.setSelectedItemId(R.id.nav_publico);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, voltarParaPublico);

        nav.setOnItemSelectedListener(item -> {
            mostrarAba(item.getItemId());
            return true;
        });
        int aba = estado != null ? estado.getInt(ESTADO_ABA, R.id.nav_publico) : R.id.nav_publico;
        nav.setSelectedItemId(aba);
        mostrarAba(aba);
    }

    private void mostrarAba(int id) {
        abaAtual = id;
        findViewById(R.id.abaPublico).setVisibility(id == R.id.nav_publico ? View.VISIBLE : View.GONE);
        findViewById(R.id.abaConversas).setVisibility(id == R.id.nav_conversas ? View.VISIBLE : View.GONE);
        findViewById(R.id.abaPessoas).setVisibility(id == R.id.nav_pessoas ? View.VISIBLE : View.GONE);

        if (id == R.id.nav_conversas) {
            txtTitulo.setText(R.string.titulo_conversas);
            txtSubtitulo.setText(R.string.subtitulo_conversas);
        } else if (id == R.id.nav_pessoas) {
            txtTitulo.setText(R.string.titulo_pessoas);
            txtSubtitulo.setText(R.string.subtitulo_pessoas);
        } else {
            txtTitulo.setText(R.string.titulo_publico);
            txtSubtitulo.setText(R.string.subtitulo_publico);
        }
        if (id != R.id.nav_publico) {
            Janela.esconderTeclado(edtMensagemPublico);
        }
        voltarParaPublico.setEnabled(id != R.id.nav_publico);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_ABA, abaAtual);
    }

    // ------------------------------------------------------------------ aba publico

    private void configurarPublico() {
        recyclerPublico = findViewById(R.id.recyclerPublico);
        layoutPublico = new LinearLayoutManager(this);
        layoutPublico.setStackFromEnd(true);
        recyclerPublico.setLayoutManager(layoutPublico);
        mensagemAdapter = new MensagemAdapter(meuUid, true, this);
        recyclerPublico.setAdapter(mensagemAdapter);
        // Quando o teclado abre a lista encolhe; mantem a ultima mensagem visivel.
        recyclerPublico.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob) recyclerPublico.post(this::rolarPublicoParaFim);
        });

        RecyclerView faixa = findViewById(R.id.recyclerFaixaPessoas);
        faixa.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        faixaAdapter = new PessoaAdapter(true, this);
        faixa.setAdapter(faixaAdapter);

        edtMensagemPublico = findViewById(R.id.edtMensagemPublico);
        btnEnviarPublico = findViewById(R.id.btnEnviarPublico);
        BotaoEnviar.ligar(edtMensagemPublico, btnEnviarPublico);
        btnEnviarPublico.setOnClickListener(v -> enviarPublico());
    }

    private void observarPublico() {
        publicoQuery = Banco.chatPublico().orderByKey().limitToLast(LIMITE_PUBLICO);
        publicoListener = publicoQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String anterior) {
                Mensagem m = Mensagem.de(snapshot);
                if (m.getTexto().isEmpty()) return;
                boolean estavaNoFim = layoutPublico.findLastVisibleItemPosition() >= mensagemAdapter.getItemCount() - 2;
                mensagemAdapter.adicionar(m);
                findViewById(R.id.vazioPublico).setVisibility(View.GONE);
                if (!publicoCarregado || estavaNoFim || meuUid.equals(m.getUid())) {
                    rolarPublicoParaFim();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String anterior) {
                mensagemAdapter.adicionar(Mensagem.de(snapshot));
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String anterior) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                findViewById(R.id.progressPublico).setVisibility(View.GONE);
            }
        });
        // O evento de valor chega depois de todos os onChildAdded iniciais.
        publicoQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                publicoCarregado = true;
                findViewById(R.id.progressPublico).setVisibility(View.GONE);
                findViewById(R.id.vazioPublico).setVisibility(mensagemAdapter.temMensagens() ? View.GONE : View.VISIBLE);
                rolarPublicoParaFim();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                findViewById(R.id.progressPublico).setVisibility(View.GONE);
            }
        });
    }

    private void enviarPublico() {
        String texto = Texto.de(edtMensagemPublico);
        if (texto.isEmpty()) return;
        if (eu == null) {
            Toast.makeText(this, R.string.carregando_perfil, Toast.LENGTH_SHORT).show();
            return;
        }
        edtMensagemPublico.setText("");
        Banco.enviarPublico(meuUid, eu.getNome(), texto)
                .addOnFailureListener(e -> falhaNoEnvio(e, texto, edtMensagemPublico));
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

    private void rolarPublicoParaFim() {
        int total = mensagemAdapter.getItemCount();
        if (total == 0) return;
        if (publicoCarregado) {
            recyclerPublico.smoothScrollToPosition(total - 1);
        } else {
            recyclerPublico.scrollToPosition(total - 1);
        }
    }

    // ------------------------------------------------------------------ aba conversas

    private void configurarConversas() {
        RecyclerView recycler = findViewById(R.id.recyclerConversas);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        conversaAdapter = new ConversaAdapter(meuUid, this);
        recycler.setAdapter(conversaAdapter);
        findViewById(R.id.btnEncontrarPessoas).setOnClickListener(v -> nav.setSelectedItemId(R.id.nav_pessoas));
    }

    private void observarConversas() {
        conversasRef = Banco.minhasConversas(meuUid);
        conversasListener = conversasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversas.clear();
                int naoLidas = 0;
                for (DataSnapshot filho : snapshot.getChildren()) {
                    Conversa c = Conversa.de(filho);
                    if (c.getUltimaEm() <= 0) continue;
                    conversas.add(c);
                    naoLidas += c.getNaoLidas();
                }
                Collections.sort(conversas, (a, b) -> Long.compare(b.getUltimaEm(), a.getUltimaEm()));
                conversaAdapter.setDados(conversas, usuarios);
                findViewById(R.id.vazioConversas).setVisibility(conversas.isEmpty() ? View.VISIBLE : View.GONE);
                atualizarSelo(naoLidas);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void atualizarSelo(int naoLidas) {
        BadgeDrawable selo = nav.getOrCreateBadge(R.id.nav_conversas);
        selo.setVisible(naoLidas > 0);
        if (naoLidas > 0) {
            selo.setNumber(naoLidas);
        }
    }

    // ------------------------------------------------------------------ aba pessoas

    private void configurarPessoas() {
        RecyclerView recycler = findViewById(R.id.recyclerPessoas);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        pessoaAdapter = new PessoaAdapter(false, this);
        recycler.setAdapter(pessoaAdapter);
        vazioPessoas = findViewById(R.id.vazioPessoas);

        edtBusca = findViewById(R.id.edtBusca);
        edtBusca.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarPessoas();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        edtBusca.setOnEditorActionListener((v, acao, evento) -> {
            Janela.esconderTeclado(v);
            return true;
        });
    }

    private void observarUsuarios() {
        usuariosRef = Banco.usuarios();
        usuariosListener = usuariosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usuarios.clear();
                pessoas.clear();
                for (DataSnapshot filho : snapshot.getChildren()) {
                    Usuario u = Usuario.de(filho);
                    if (u.getNome().isEmpty()) continue;
                    usuarios.put(u.getUid(), u);
                    if (meuUid.equals(u.getUid())) {
                        eu = u;
                    } else {
                        pessoas.add(u);
                    }
                }
                // Online primeiro, depois ordem alfabetica.
                Collections.sort(pessoas, (a, b) -> {
                    if (a.isOnline() != b.isOnline()) return a.isOnline() ? -1 : 1;
                    return a.getNome().compareToIgnoreCase(b.getNome());
                });

                if (eu != null) {
                    Avatar.aplicar(txtMeuAvatar, meuUid, eu.getNome());
                }
                faixaAdapter.setItens(pessoas.subList(0, Math.min(LIMITE_FAIXA, pessoas.size())));
                findViewById(R.id.faixaPessoas).setVisibility(pessoas.isEmpty() ? View.GONE : View.VISIBLE);
                filtrarPessoas();
                conversaAdapter.setDados(conversas, usuarios);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void filtrarPessoas() {
        String busca = Texto.de(edtBusca);
        String termo = Texto.normalizar(busca);
        List<Usuario> filtradas = new ArrayList<>();
        for (Usuario u : pessoas) {
            if (termo.isEmpty() || Texto.normalizar(u.getNome()).contains(termo)
                    || Texto.normalizar(u.getRecado()).contains(termo)) {
                filtradas.add(u);
            }
        }
        pessoaAdapter.setItens(filtradas);
        if (filtradas.isEmpty()) {
            vazioPessoas.setText(termo.isEmpty()
                    ? getString(R.string.pessoas_vazio)
                    : getString(R.string.pessoas_nada_encontrado, busca));
            vazioPessoas.setVisibility(View.VISIBLE);
        } else {
            vazioPessoas.setVisibility(View.GONE);
        }
    }

    /** Conta criada mas perfil nao gravado (ex.: sem internet no cadastro): cria um basico. */
    private void garantirPerfil(final FirebaseUser user) {
        Banco.usuario(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) return;
                String email = user.getEmail() != null ? user.getEmail() : "";
                String nome = email.contains("@") ? email.substring(0, email.indexOf('@')) : "";
                if (nome.length() < 2) nome = getString(R.string.usuario);
                if (nome.length() > 40) nome = nome.substring(0, 40);
                // O onDisconnect registrado antes falhou (perfil nao existia); registra de novo.
                Banco.criarPerfil(user.getUid(), nome).addOnSuccessListener(v -> Presenca.renovar());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ------------------------------------------------------------------ cliques

    @Override
    public void onAutorClick(String uid, String nome) {
        Usuario conhecido = usuarios.get(uid);
        PerfilSheet.mostrar(this, uid, conhecido != null ? conhecido.getNome() : nome);
    }

    @Override
    public void onPessoaClick(Usuario usuario) {
        PerfilSheet.mostrar(this, usuario.getUid(), usuario.getNome());
    }

    @Override
    public void onConversarClick(Usuario usuario) {
        ChatActivity.abrir(this, usuario.getUid(), usuario.getNome());
    }

    @Override
    public void onConversaClick(Conversa conversa, String nomeContato) {
        ChatActivity.abrir(this, conversa.getContatoUid(), nomeContato);
    }

    @Override
    public void onConversaFotoClick(Conversa conversa, String nomeContato) {
        PerfilSheet.mostrar(this, conversa.getContatoUid(), nomeContato);
    }

    @Override
    protected void onDestroy() {
        if (usuariosRef != null && usuariosListener != null) usuariosRef.removeEventListener(usuariosListener);
        if (publicoQuery != null && publicoListener != null) publicoQuery.removeEventListener(publicoListener);
        if (conversasRef != null && conversasListener != null) conversasRef.removeEventListener(conversasListener);
        super.onDestroy();
    }
}

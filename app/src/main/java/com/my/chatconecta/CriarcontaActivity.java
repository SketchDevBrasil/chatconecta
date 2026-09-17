package com.my.chatconecta;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.my.chatconecta.data.Banco;
import com.my.chatconecta.data.FirebaseInit;
import com.my.chatconecta.data.Presenca;
import com.my.chatconecta.util.Janela;
import com.my.chatconecta.util.Texto;

public class CriarcontaActivity extends AppCompatActivity {

    private TextInputLayout tilNome;
    private TextInputLayout tilEmail;
    private TextInputLayout tilSenha;
    private TextInputLayout tilConfirmar;
    private TextInputEditText edtNome;
    private TextInputEditText edtEmail;
    private TextInputEditText edtSenha;
    private TextInputEditText edtConfirmar;
    private MaterialButton btnCadastrar;
    private ProgressBar progress;
    private FirebaseAuth auth;
    private boolean carregando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criarconta);
        Janela.telaCheia(this, true);
        Janela.aplicarEspacos(findViewById(R.id.raizCriarConta), findViewById(R.id.headerCriarConta),
                findViewById(R.id.raizCriarConta));

        FirebaseInit.ensure(this);
        auth = FirebaseAuth.getInstance();

        tilNome = findViewById(R.id.tilNome);
        tilEmail = findViewById(R.id.tilEmail);
        tilSenha = findViewById(R.id.tilSenha);
        tilConfirmar = findViewById(R.id.tilConfirmarSenha);
        edtNome = findViewById(R.id.edtNome);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        edtConfirmar = findViewById(R.id.edtConfirmarSenha);
        btnCadastrar = findViewById(R.id.btnCadastrar);
        progress = findViewById(R.id.progressCadastro);

        btnCadastrar.setOnClickListener(v -> cadastrar());
        edtConfirmar.setOnEditorActionListener((v, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_DONE) {
                cadastrar();
                return true;
            }
            return false;
        });
        findViewById(R.id.btnVoltarCriar).setOnClickListener(v -> finish());
        findViewById(R.id.txtJaTenhoConta).setOnClickListener(v -> finish());
    }

    private void cadastrar() {
        if (carregando) {
            return;
        }
        for (TextInputLayout campo : new TextInputLayout[]{tilNome, tilEmail, tilSenha, tilConfirmar}) {
            campo.setError(null);
        }
        final String nome = Texto.de(edtNome).replaceAll("\\s+", " ");
        String email = Texto.de(edtEmail);
        String senha = edtSenha.getText() != null ? edtSenha.getText().toString() : "";
        String confirmar = edtConfirmar.getText() != null ? edtConfirmar.getText().toString() : "";

        if (nome.length() < 2 || nome.length() > 40) {
            tilNome.setError(getString(R.string.erro_nome));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.erro_email));
            return;
        }
        if (senha.length() < 6) {
            tilSenha.setError(getString(R.string.erro_senha_curta));
            return;
        }
        if (!senha.equals(confirmar)) {
            tilConfirmar.setError(getString(R.string.erro_senhas_diferentes));
            return;
        }

        Janela.esconderTeclado(edtConfirmar);
        setCarregando(true);
        auth.createUserWithEmailAndPassword(email, senha)
                .addOnSuccessListener(resultado -> {
                    if (resultado.getUser() == null) {
                        abrirPrincipal();
                        return;
                    }
                    // Mesmo se o perfil falhar agora, a MainActivity recria um perfil basico.
                    Banco.criarPerfil(resultado.getUser().getUid(), nome)
                            .addOnCompleteListener(t -> abrirPrincipal());
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    setCarregando(false);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        tilEmail.setError(getString(R.string.erro_email_em_uso));
                    } else if (e instanceof FirebaseAuthWeakPasswordException) {
                        tilSenha.setError(getString(R.string.erro_senha_curta));
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        tilEmail.setError(getString(R.string.erro_email));
                    } else if (e instanceof FirebaseNetworkException) {
                        Toast.makeText(this, R.string.erro_rede, Toast.LENGTH_LONG).show();
                    } else if (e instanceof FirebaseTooManyRequestsException) {
                        Toast.makeText(this, R.string.erro_muitas_tentativas, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.erro_generico, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void abrirPrincipal() {
        if (isFinishing()) return;
        Presenca.entrar();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setCarregando(boolean ativo) {
        carregando = ativo;
        btnCadastrar.setText(ativo ? "" : getString(R.string.acao_criar_conta));
        progress.setVisibility(ativo ? View.VISIBLE : View.GONE);
        for (TextInputEditText campo : new TextInputEditText[]{edtNome, edtEmail, edtSenha, edtConfirmar}) {
            campo.setEnabled(!ativo);
        }
    }
}

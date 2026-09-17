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
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.my.chatconecta.data.FirebaseInit;
import com.my.chatconecta.data.Presenca;
import com.my.chatconecta.util.Janela;
import com.my.chatconecta.util.Texto;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputLayout tilSenha;
    private TextInputEditText edtEmail;
    private TextInputEditText edtSenha;
    private MaterialButton btnEntrar;
    private ProgressBar progress;
    private FirebaseAuth auth;
    private boolean carregando;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Janela.telaCheia(this, true);
        Janela.aplicarEspacos(findViewById(R.id.raizLogin), findViewById(R.id.headerLogin), findViewById(R.id.raizLogin));

        FirebaseInit.ensure(this);
        auth = FirebaseAuth.getInstance();

        tilEmail = findViewById(R.id.tilEmail);
        tilSenha = findViewById(R.id.tilSenha);
        edtEmail = findViewById(R.id.edtEmail);
        edtSenha = findViewById(R.id.edtSenha);
        btnEntrar = findViewById(R.id.btnEntrar);
        progress = findViewById(R.id.progressLogin);

        btnEntrar.setOnClickListener(v -> entrar());
        edtSenha.setOnEditorActionListener((v, acao, evento) -> {
            if (acao == EditorInfo.IME_ACTION_DONE) {
                entrar();
                return true;
            }
            return false;
        });
        findViewById(R.id.txtEsqueciSenha).setOnClickListener(v -> redefinirSenha());
        findViewById(R.id.txtCriarConta).setOnClickListener(v ->
                startActivity(new Intent(this, CriarcontaActivity.class)));
    }

    private void entrar() {
        if (carregando) {
            return;
        }
        tilEmail.setError(null);
        tilSenha.setError(null);
        String email = Texto.de(edtEmail);
        String senha = edtSenha.getText() != null ? edtSenha.getText().toString() : "";

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.erro_email));
            return;
        }
        if (senha.isEmpty()) {
            tilSenha.setError(getString(R.string.erro_senha_vazia));
            return;
        }

        Janela.esconderTeclado(edtSenha);
        setCarregando(true);
        auth.signInWithEmailAndPassword(email, senha)
                .addOnSuccessListener(resultado -> {
                    if (isFinishing()) return;
                    Presenca.entrar();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    setCarregando(false);
                    if (e instanceof FirebaseAuthInvalidCredentialsException
                            || e instanceof FirebaseAuthInvalidUserException) {
                        tilSenha.setError(getString(R.string.erro_login));
                    } else {
                        Toast.makeText(this, mensagemDeErro(e), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void redefinirSenha() {
        String email = Texto.de(edtEmail);
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.erro_email));
            edtEmail.requestFocus();
            return;
        }
        tilEmail.setError(null);
        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> Toast.makeText(this,
                        getString(R.string.reset_enviado, email), Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        e instanceof FirebaseNetworkException ? R.string.erro_rede : R.string.reset_erro,
                        Toast.LENGTH_LONG).show());
    }

    private int mensagemDeErro(Exception e) {
        if (e instanceof FirebaseNetworkException) return R.string.erro_rede;
        if (e instanceof FirebaseTooManyRequestsException) return R.string.erro_muitas_tentativas;
        return R.string.erro_generico;
    }

    private void setCarregando(boolean ativo) {
        carregando = ativo;
        btnEntrar.setText(ativo ? "" : getString(R.string.acao_entrar));
        progress.setVisibility(ativo ? View.VISIBLE : View.GONE);
        edtEmail.setEnabled(!ativo);
        edtSenha.setEnabled(!ativo);
    }
}

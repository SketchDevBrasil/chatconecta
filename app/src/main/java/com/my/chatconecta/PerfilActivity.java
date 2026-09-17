package com.my.chatconecta;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.my.chatconecta.data.Banco;
import com.my.chatconecta.data.FirebaseInit;
import com.my.chatconecta.data.Sessao;
import com.my.chatconecta.model.Usuario;
import com.my.chatconecta.util.Avatar;
import com.my.chatconecta.util.Janela;
import com.my.chatconecta.util.Texto;

public class PerfilActivity extends AppCompatActivity {

    private String meuUid;
    private TextView txtAvatar;
    private TextView txtNome;
    private TextInputLayout tilNome;
    private TextInputEditText edtNome;
    private TextInputEditText edtRecado;
    private MaterialButton btnSalvar;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseInit.ensure(this);
        FirebaseUser user = FirebaseInit.configurado() ? FirebaseAuth.getInstance().getCurrentUser() : null;
        if (user == null) {
            finish();
            return;
        }
        meuUid = user.getUid();

        setContentView(R.layout.activity_perfil);
        Janela.telaCheia(this, false);
        Janela.aplicarEspacos(findViewById(R.id.raizPerfil), findViewById(R.id.topoPerfil), findViewById(R.id.raizPerfil));

        txtAvatar = findViewById(R.id.txtAvatarPerfil);
        txtNome = findViewById(R.id.txtNomePerfil);
        tilNome = findViewById(R.id.tilNomePerfil);
        edtNome = findViewById(R.id.edtNomePerfil);
        edtRecado = findViewById(R.id.edtRecadoPerfil);
        btnSalvar = findViewById(R.id.btnSalvarPerfil);
        progress = findViewById(R.id.progressPerfil);

        ((TextView) findViewById(R.id.txtEmailPerfil)).setText(user.getEmail());
        Avatar.aplicar(txtAvatar, meuUid, "");

        // Preview ao vivo das iniciais enquanto digita o nome.
        edtNome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Avatar.aplicar(txtAvatar, meuUid, s.toString());
                txtNome.setText(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.btnVoltarPerfil).setOnClickListener(v -> finish());
        btnSalvar.setOnClickListener(v -> salvar());
        findViewById(R.id.btnSairPerfil).setOnClickListener(v -> confirmarSaida());

        carregarPerfil();
    }

    private void carregarPerfil() {
        Banco.usuario(meuUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || isFinishing()) return;
                Usuario eu = Usuario.de(snapshot);
                edtNome.setText(eu.getNome());
                edtRecado.setText(eu.getRecado());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void salvar() {
        tilNome.setError(null);
        String nome = Texto.de(edtNome).replaceAll("\\s+", " ");
        String recado = Texto.de(edtRecado);
        if (nome.length() < 2 || nome.length() > 40) {
            tilNome.setError(getString(R.string.erro_nome));
            return;
        }
        if (recado.length() > 120) {
            recado = recado.substring(0, 120);
        }

        Janela.esconderTeclado(edtNome);
        setCarregando(true);
        Banco.atualizarPerfil(meuUid, nome, recado)
                .addOnSuccessListener(v -> {
                    if (isFinishing()) return;
                    setCarregando(false);
                    Toast.makeText(this, R.string.perfil_salvo, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing()) return;
                    setCarregando(false);
                    Toast.makeText(this, R.string.perfil_erro, Toast.LENGTH_LONG).show();
                });
    }

    private void confirmarSaida() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.sair_da_conta)
                .setMessage(R.string.sair_confirmar)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.sair, (dialog, which) -> Sessao.sair(this))
                .show();
    }

    private void setCarregando(boolean ativo) {
        btnSalvar.setText(ativo ? "" : getString(R.string.salvar_alteracoes));
        btnSalvar.setClickable(!ativo);
        progress.setVisibility(ativo ? View.VISIBLE : View.GONE);
    }
}

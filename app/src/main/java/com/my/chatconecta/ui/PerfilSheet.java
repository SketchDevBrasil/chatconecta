package com.my.chatconecta.ui;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.my.chatconecta.ChatActivity;
import com.my.chatconecta.PerfilActivity;
import com.my.chatconecta.R;
import com.my.chatconecta.data.Banco;
import com.my.chatconecta.model.Usuario;
import com.my.chatconecta.util.Avatar;
import com.my.chatconecta.util.Tempo;

/**
 * Folha que sobe ao tocar na foto de alguem: mostra o perfil e o botao "Abrir chat".
 * Se a foto for a do proprio usuario, o botao vira "Editar meu perfil".
 */
public final class PerfilSheet {

    private PerfilSheet() {
    }

    public static void mostrar(final Activity activity, final String uid, String nome) {
        if (uid == null || uid.isEmpty()) {
            return;
        }
        final BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View v = LayoutInflater.from(activity).inflate(R.layout.sheet_perfil, null);
        dialog.setContentView(v);

        final TextView txtAvatar = v.findViewById(R.id.txtAvatarSheet);
        final View pontoOnline = v.findViewById(R.id.pontoOnlineSheet);
        final TextView txtNome = v.findViewById(R.id.txtNomeSheet);
        final TextView txtStatus = v.findViewById(R.id.txtStatusSheet);
        final TextView txtRecado = v.findViewById(R.id.txtRecadoSheet);
        MaterialButton btnAcao = v.findViewById(R.id.btnAcaoSheet);
        final String[] nomeAtual = {nome};

        Avatar.aplicar(txtAvatar, uid, nome);
        txtNome.setText(nome);

        boolean ehVoce = uid.equals(Banco.meuUid());
        if (ehVoce) {
            btnAcao.setText(R.string.editar_meu_perfil);
            btnAcao.setIconResource(R.drawable.ic_edit);
            btnAcao.setOnClickListener(x -> {
                dialog.dismiss();
                activity.startActivity(new Intent(activity, PerfilActivity.class));
            });
        } else {
            btnAcao.setOnClickListener(x -> {
                dialog.dismiss();
                ChatActivity.abrir(activity, uid, nomeAtual[0]);
            });
        }

        // Escuta ao vivo enquanto a folha esta aberta (status online muda na hora).
        final DatabaseReference ref = Banco.usuario(uid);
        final ValueEventListener ouvinte = ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    return;
                }
                Usuario u = Usuario.de(snapshot);
                nomeAtual[0] = u.getNome();
                Avatar.aplicar(txtAvatar, uid, u.getNome());
                txtNome.setText(u.getNome());
                txtStatus.setText(Tempo.status(activity, u));
                txtStatus.setTextColor(ContextCompat.getColor(activity,
                        u.isOnline() ? R.color.online : R.color.text_secondary));
                pontoOnline.setVisibility(u.isOnline() ? View.VISIBLE : View.GONE);
                txtRecado.setVisibility(u.getRecado().isEmpty() ? View.GONE : View.VISIBLE);
                txtRecado.setText(u.getRecado());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
        dialog.setOnDismissListener(d -> ref.removeEventListener(ouvinte));
        dialog.show();
    }
}

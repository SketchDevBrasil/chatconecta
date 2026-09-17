package com.my.chatconecta.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Status online/offline usando somente o Realtime Database: ".info/connected" avisa
 * quando o app conectou e o onDisconnect() grava offline no servidor se a conexao cair.
 * ChatApp chama entrar()/sair() quando o app vai para frente ou para o fundo.
 */
public final class Presenca {

    private static DatabaseReference conexaoRef;
    private static ValueEventListener ouvinte;
    private static String uidAtual;

    private Presenca() {
    }

    public static synchronized void entrar() {
        if (!FirebaseInit.configurado()) {
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        if (ouvinte != null) {
            if (user.getUid().equals(uidAtual)) {
                return;
            }
            pararOuvinte();
        }
        uidAtual = user.getUid();
        final DatabaseReference eu = Banco.usuario(uidAtual);
        conexaoRef = Banco.db().getReference(".info/connected");
        ouvinte = conexaoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    return;
                }
                eu.onDisconnect().updateChildren(dadosOffline());
                eu.child("online").setValue(true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    /** Registra de novo o onDisconnect (ex.: logo depois de criar o perfil que faltava). */
    public static synchronized void renovar() {
        pararOuvinte();
        uidAtual = null;
        entrar();
    }

    /** Marca offline. Devolve a escrita para quem precisa esperar (logout) ou null sem sessao. */
    @Nullable
    public static synchronized Task<Void> sair() {
        if (uidAtual == null) {
            return null;
        }
        pararOuvinte();
        Task<Void> escrita = Banco.usuario(uidAtual).updateChildren(dadosOffline());
        uidAtual = null;
        return escrita;
    }

    private static void pararOuvinte() {
        if (conexaoRef != null && ouvinte != null) {
            conexaoRef.removeEventListener(ouvinte);
        }
        conexaoRef = null;
        ouvinte = null;
    }

    private static Map<String, Object> dadosOffline() {
        Map<String, Object> dados = new HashMap<>();
        dados.put("online", false);
        dados.put("vistoEm", ServerValue.TIMESTAMP);
        return dados;
    }
}

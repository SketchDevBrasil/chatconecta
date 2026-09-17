package com.my.chatconecta.model;

import com.google.firebase.database.DataSnapshot;

/** Perfil publico em usuarios/{uid}. O e-mail nao fica aqui: so o proprio dono ve pelo FirebaseAuth. */
public class Usuario {

    private String uid = "";
    private String nome = "";
    private String recado = "";
    private boolean online;
    private long vistoEm;

    public static Usuario de(DataSnapshot snapshot) {
        Usuario u = new Usuario();
        u.uid = snapshot.getKey();
        u.nome = Leitura.texto(snapshot.child("nome").getValue());
        u.recado = Leitura.texto(snapshot.child("recado").getValue());
        u.online = Boolean.TRUE.equals(snapshot.child("online").getValue());
        u.vistoEm = Leitura.numero(snapshot.child("vistoEm").getValue());
        return u;
    }

    public String getUid() {
        return uid;
    }

    public String getNome() {
        return nome;
    }

    public String getRecado() {
        return recado;
    }

    public boolean isOnline() {
        return online;
    }

    public long getVistoEm() {
        return vistoEm;
    }
}

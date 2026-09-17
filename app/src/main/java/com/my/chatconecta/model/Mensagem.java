package com.my.chatconecta.model;

import com.google.firebase.database.DataSnapshot;

/** Mensagem do chat publico (com nome do autor) ou privado (nome vazio). */
public class Mensagem {

    private String id = "";
    private String uid = "";
    private String nome = "";
    private String texto = "";
    private long enviadoEm;

    public static Mensagem de(DataSnapshot snapshot) {
        Mensagem m = new Mensagem();
        m.id = snapshot.getKey();
        m.uid = Leitura.texto(snapshot.child("uid").getValue());
        m.nome = Leitura.texto(snapshot.child("nome").getValue());
        m.texto = Leitura.texto(snapshot.child("texto").getValue());
        m.enviadoEm = Leitura.numero(snapshot.child("enviadoEm").getValue());
        return m;
    }

    public String getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public String getNome() {
        return nome;
    }

    public String getTexto() {
        return texto;
    }

    public long getEnviadoEm() {
        return enviadoEm;
    }
}

package com.my.chatconecta.model;

import com.google.firebase.database.DataSnapshot;

/** Item de usuarioConversas/{meuUid}/{contatoUid}. O nome do contato vem de usuarios/. */
public class Conversa {

    private String contatoUid = "";
    private String ultimaMensagem = "";
    private String ultimoUid = "";
    private long ultimaEm;
    private int naoLidas;

    public static Conversa de(DataSnapshot snapshot) {
        Conversa c = new Conversa();
        c.contatoUid = snapshot.getKey();
        c.ultimaMensagem = Leitura.texto(snapshot.child("ultimaMensagem").getValue());
        c.ultimoUid = Leitura.texto(snapshot.child("ultimoUid").getValue());
        c.ultimaEm = Leitura.numero(snapshot.child("ultimaEm").getValue());
        c.naoLidas = (int) Leitura.numero(snapshot.child("naoLidas").getValue());
        return c;
    }

    public String getContatoUid() {
        return contatoUid;
    }

    public String getUltimaMensagem() {
        return ultimaMensagem;
    }

    public String getUltimoUid() {
        return ultimoUid;
    }

    public long getUltimaEm() {
        return ultimaEm;
    }

    public int getNaoLidas() {
        return naoLidas;
    }
}

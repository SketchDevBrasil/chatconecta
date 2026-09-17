package com.my.chatconecta.model;

/** Leitura tolerante dos valores crus do Realtime Database (evita crash com dado faltando). */
final class Leitura {

    private Leitura() {
    }

    static String texto(Object valor) {
        return valor instanceof String ? (String) valor : "";
    }

    static long numero(Object valor) {
        return valor instanceof Number ? ((Number) valor).longValue() : 0L;
    }
}

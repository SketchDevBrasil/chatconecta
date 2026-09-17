package com.my.chatconecta.util;

import android.widget.EditText;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {

    private Texto() {
    }

    public static String de(EditText campo) {
        return campo.getText() != null ? campo.getText().toString().trim() : "";
    }

    /** Busca sem diferenciar maiusculas e acentos: "Joao" encontra "JOAO". */
    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}

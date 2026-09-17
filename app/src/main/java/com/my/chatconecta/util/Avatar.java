package com.my.chatconecta.util;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import java.util.Locale;

/**
 * Sem Firebase Storage, a "foto" de perfil e um circulo com as iniciais do nome.
 * A cor sai do uid, entao cada pessoa tem sempre a mesma cor em todas as telas.
 */
public final class Avatar {

    private static final int[] CORES = {
            0xFF6C5CE7, 0xFF0EA371, 0xFF1E78D6, 0xFFE0603C,
            0xFFC8337E, 0xFFC9731F, 0xFF0E9AA7, 0xFF8B3FC4
    };

    private Avatar() {
    }

    public static int cor(String uid) {
        if (uid == null || uid.isEmpty()) {
            return CORES[0];
        }
        return CORES[Math.abs(uid.hashCode() % CORES.length)];
    }

    public static String iniciais(String nome) {
        String limpo = nome == null ? "" : nome.trim();
        if (limpo.isEmpty()) {
            return "?";
        }
        String[] partes = limpo.split("\\s+");
        String iniciais = primeiraLetra(partes[0]);
        if (partes.length > 1) {
            iniciais += primeiraLetra(partes[partes.length - 1]);
        }
        return iniciais.toUpperCase(Locale.getDefault());
    }

    public static void aplicar(TextView view, String uid, String nome) {
        int cor = cor(uid);
        GradientDrawable fundo = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{ColorUtils.blendARGB(cor, Color.WHITE, 0.22f), cor});
        fundo.setShape(GradientDrawable.OVAL);
        view.setBackground(fundo);
        view.setText(iniciais(nome));
    }

    public static String primeiroNome(String nome) {
        String limpo = nome == null ? "" : nome.trim();
        int espaco = limpo.indexOf(' ');
        return espaco > 0 ? limpo.substring(0, espaco) : limpo;
    }

    private static String primeiraLetra(String palavra) {
        return new String(Character.toChars(palavra.codePointAt(0)));
    }
}

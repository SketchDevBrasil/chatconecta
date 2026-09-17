package com.my.chatconecta.util;

import android.content.Context;

import com.my.chatconecta.R;
import com.my.chatconecta.model.Usuario;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Tempo {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private Tempo() {
    }

    public static String hora(long ms) {
        return new SimpleDateFormat("HH:mm", PT_BR).format(new Date(ms));
    }

    public static boolean mesmoDia(long a, long b) {
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(a);
        Calendar cb = Calendar.getInstance();
        cb.setTimeInMillis(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    /** Separador de data dentro do chat: Hoje, Ontem, 12 de setembro. */
    public static String dia(Context context, long ms) {
        long agora = System.currentTimeMillis();
        if (mesmoDia(ms, agora)) {
            return context.getString(R.string.hoje);
        }
        if (ehOntem(ms)) {
            return context.getString(R.string.ontem);
        }
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ms);
        boolean mesmoAno = c.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR);
        return new SimpleDateFormat(mesmoAno ? "d 'de' MMMM" : "d 'de' MMMM 'de' yyyy", PT_BR).format(new Date(ms));
    }

    /** Horario curto da lista de conversas: 14:32, Ontem, 03/09/26. */
    public static String curta(Context context, long ms) {
        if (ms <= 0) {
            return "";
        }
        if (mesmoDia(ms, System.currentTimeMillis())) {
            return hora(ms);
        }
        if (ehOntem(ms)) {
            return context.getString(R.string.ontem);
        }
        return new SimpleDateFormat("dd/MM/yy", PT_BR).format(new Date(ms));
    }

    public static String status(Context context, Usuario usuario) {
        if (usuario == null) {
            return "";
        }
        if (usuario.isOnline()) {
            return context.getString(R.string.status_online);
        }
        long visto = usuario.getVistoEm();
        if (visto <= 0) {
            return context.getString(R.string.status_offline);
        }
        if (mesmoDia(visto, System.currentTimeMillis())) {
            return context.getString(R.string.status_visto_hoje, hora(visto));
        }
        if (ehOntem(visto)) {
            return context.getString(R.string.status_visto_ontem, hora(visto));
        }
        return context.getString(R.string.status_visto_data,
                new SimpleDateFormat("dd/MM", PT_BR).format(new Date(visto)), hora(visto));
    }

    private static boolean ehOntem(long ms) {
        Calendar ontem = Calendar.getInstance();
        ontem.add(Calendar.DAY_OF_YEAR, -1);
        return mesmoDia(ms, ontem.getTimeInMillis());
    }
}

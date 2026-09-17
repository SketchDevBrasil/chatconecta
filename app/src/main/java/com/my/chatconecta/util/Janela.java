package com.my.chatconecta.util;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Com targetSdk 35+ o Android 15/16 obriga o app a desenhar atras das barras do sistema
 * (edge-to-edge). Aqui ligamos isso em todas as versoes e aplicamos os espacos das
 * barras e do teclado manualmente, para a tela ficar igual do Android 6 ao 16.
 */
public final class Janela {

    private Janela() {
    }

    /** @param fundoColorido true quando o topo da tela e um gradiente (icones da status bar brancos). */
    public static void telaCheia(Activity activity, boolean fundoColorido) {
        Window window = activity.getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        // Antes do Android 8.1 os icones da barra de navegacao sao sempre brancos.
        window.setNavigationBarColor(Build.VERSION.SDK_INT >= 27 ? Color.TRANSPARENT : 0x99000000);

        boolean noturno = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controle = WindowCompat.getInsetsController(window, window.getDecorView());
        controle.setAppearanceLightStatusBars(!fundoColorido && !noturno);
        controle.setAppearanceLightNavigationBars(Build.VERSION.SDK_INT >= 27 && !noturno);
    }

    /**
     * @param topo recebe a altura da status bar como padding superior
     * @param base recebe a barra de navegacao ou o teclado (o que for maior) como padding inferior
     */
    public static void aplicarEspacos(View raiz, @Nullable final View topo, @Nullable final View base) {
        final int topoOriginal = topo != null ? topo.getPaddingTop() : 0;
        final int baseOriginal = base != null ? base.getPaddingBottom() : 0;
        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int teclado = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(barras.left, v.getPaddingTop(), barras.right, v.getPaddingBottom());
            if (topo != null) {
                topo.setPadding(topo.getPaddingLeft(), topoOriginal + barras.top,
                        topo.getPaddingRight(), topo.getPaddingBottom());
            }
            if (base != null) {
                base.setPadding(base.getPaddingLeft(), base.getPaddingTop(),
                        base.getPaddingRight(), baseOriginal + Math.max(barras.bottom, teclado));
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    public static void esconderTeclado(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}

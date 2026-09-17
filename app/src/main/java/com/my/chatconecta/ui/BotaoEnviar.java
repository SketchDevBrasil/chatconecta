package com.my.chatconecta.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

/** O botao de enviar fica apagado enquanto a caixa de mensagem esta vazia. */
public final class BotaoEnviar {

    private static final float APAGADO = 0.45f;

    private BotaoEnviar() {
    }

    public static void ligar(EditText campo, final View botao) {
        botao.setEnabled(false);
        botao.setAlpha(APAGADO);
        campo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean temTexto = s.toString().trim().length() > 0;
                if (botao.isEnabled() != temTexto) {
                    botao.setEnabled(temTexto);
                    botao.animate().alpha(temTexto ? 1f : APAGADO).setDuration(120).start();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}

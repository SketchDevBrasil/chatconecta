package com.my.chatconecta.data;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.my.chatconecta.LoginActivity;

/** A sessao em si e do FirebaseAuth; aqui fica so o logout com o status offline gravado antes. */
public final class Sessao {

    private static final long ESPERA_MAXIMA_MS = 2000;

    private Sessao() {
    }

    public static void sair(final Activity activity) {
        final Handler handler = new Handler(Looper.getMainLooper());
        final boolean[] concluido = {false};
        final Runnable concluir = () -> {
            if (concluido[0]) {
                return;
            }
            concluido[0] = true;
            handler.removeCallbacksAndMessages(null);
            Limite.sair();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        };

        // O offline precisa chegar ao servidor ainda autenticado; sem internet, desiste em 2s.
        Task<Void> escrita = Presenca.sair();
        if (escrita == null) {
            concluir.run();
            return;
        }
        escrita.addOnCompleteListener(t -> concluir.run());
        handler.postDelayed(concluir, ESPERA_MAXIMA_MS);
    }
}

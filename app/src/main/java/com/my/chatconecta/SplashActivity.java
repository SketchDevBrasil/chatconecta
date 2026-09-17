package com.my.chatconecta;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.my.chatconecta.data.FirebaseInit;
import com.my.chatconecta.util.Janela;

public class SplashActivity extends AppCompatActivity {

    private static final long DURACAO_MS = 1800;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable seguir = this::seguir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Janela.telaCheia(this, true);
        animarEntrada();

        if (!FirebaseInit.configurado()) {
            findViewById(R.id.progressSplash).setVisibility(View.GONE);
            findViewById(R.id.txtAvisoSplash).setVisibility(View.VISIBLE);
            return;
        }
        FirebaseInit.ensure(this);
        handler.postDelayed(seguir, DURACAO_MS);
    }

    private void animarEntrada() {
        View logo = findViewById(R.id.logoSplash);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();

        float deslocamento = 18 * getResources().getDisplayMetrics().density;
        long atraso = 250;
        for (int id : new int[]{R.id.txtNomeSplash, R.id.txtSloganSplash}) {
            View texto = findViewById(id);
            texto.setAlpha(0f);
            texto.setTranslationY(deslocamento);
            texto.animate().alpha(1f).translationY(0f).setStartDelay(atraso).setDuration(500)
                    .setInterpolator(new DecelerateInterpolator()).start();
            atraso += 150;
        }
    }

    private void seguir() {
        if (isFinishing()) {
            return;
        }
        boolean logado = FirebaseAuth.getInstance().getCurrentUser() != null;
        startActivity(new Intent(this, logado ? MainActivity.class : LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(seguir);
        super.onDestroy();
    }
}

package com.my.chatconecta;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.my.chatconecta.data.FirebaseInit;
import com.my.chatconecta.data.Limite;
import com.my.chatconecta.data.Presenca;

/**
 * Inicializa o Firebase uma vez e conta as telas visiveis: quando a primeira aparece o
 * usuario fica online; quando a ultima some (app foi para o fundo) fica offline.
 */
public class ChatApp extends Application {

    private int telasVisiveis = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseInit.ensure(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                telasVisiveis++;
                if (telasVisiveis == 1) {
                    Presenca.entrar();
                    Limite.entrar();
                }
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                telasVisiveis--;
                // Girar a tela recria a Activity; nao precisa piscar offline por isso.
                if (telasVisiveis == 0 && !activity.isChangingConfigurations()) {
                    Presenca.sair();
                    Limite.sair();
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }
}

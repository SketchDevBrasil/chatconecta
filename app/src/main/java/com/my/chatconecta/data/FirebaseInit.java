package com.my.chatconecta.data;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * O plugin com.google.gms.google-services nao roda no builder do Genesis, entao os
 * recursos google_app_id / google_api_key nunca sao gerados e
 * FirebaseApp.initializeApp(context) sozinho quebraria no start.
 *
 * PREENCHA os valores abaixo com os dados do app/google-services.json do SEU projeto
 * (veja o README, secao "Configurar o Firebase"):
 *   APPLICATION_ID -> client[].client_info.mobilesdk_app_id
 *   API_KEY        -> client[].api_key[].current_key
 *   PROJECT_ID     -> project_info.project_id
 *   SENDER_ID      -> project_info.project_number
 *   DATABASE_URL   -> project_info.firebase_url (URL do Realtime Database)
 */
public final class FirebaseInit {

private static final String APPLICATION_ID = "1:831138858361:android:b8bc78dff8674369935ba8";
     private static final String API_KEY = "AIzaSyCZ8wa0hDW75JMPHbc82QbYbPH_GTyp-ZY";
     private static final String PROJECT_ID = "chatconecta10";
     private static final String SENDER_ID = "831138858361";
     private static final String DATABASE_URL = "https://chatconecta10-default-rtdb.firebaseio.com";

    private static boolean iniciado = false;

    private FirebaseInit() {
    }

    /** false enquanto os valores acima ainda forem os de exemplo. */
    public static boolean configurado() {
        return !APPLICATION_ID.startsWith("COLE_AQUI") && !API_KEY.startsWith("COLE_AQUI")
                && !DATABASE_URL.contains("COLE-AQUI");
    }

    /** Idempotente: pode ser chamado em qualquer Activity sem risco de inicializacao dupla. */
    public static synchronized void ensure(Context context) {
        if (iniciado || !configurado()) {
            return;
        }
        Context app = context.getApplicationContext();
        if (FirebaseApp.getApps(app).isEmpty()) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(APPLICATION_ID)
                    .setApiKey(API_KEY)
                    .setProjectId(PROJECT_ID)
                    .setGcmSenderId(SENDER_ID)
                    .setDatabaseUrl(DATABASE_URL)
                    .build();
            FirebaseApp.initializeApp(app, options);
        }
        iniciado = true;
    }

    public static String databaseUrl() {
        return DATABASE_URL;
    }
}

package com.carpetadigital.ecommerce.Config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.*;

@Configuration
public class FireBaseConfig {

    @Bean
    public FirebaseApp initializeFirebase() throws IOException {
        String firebaseCredentials = System.getenv("SERVICE_ACCOUNT_KEY");
        InputStream serviceAccount;
        if (firebaseCredentials != null) {
            serviceAccount = new ByteArrayInputStream(firebaseCredentials.getBytes());
        } else {
            serviceAccount = new FileInputStream("src/main/resources/serviceAccountKey.json");
        }

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .setStorageBucket("cd-store-529c3.firebasestorage.app")
                .build();
        return FirebaseApp.initializeApp(options);
    }

}

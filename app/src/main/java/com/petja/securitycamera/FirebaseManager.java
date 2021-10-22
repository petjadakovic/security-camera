package com.petja.securitycamera;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

public class FirebaseManager {

    private static FirebaseManager instance;
    public String notificationToken;

    FirebaseAuth firebaseAuth;
    String token;

    public FirebaseManager() {
        instance = this;
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
    }

    public FirebaseUser firebaseUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public void initAuth() {
        FirebaseUser mUser = FirebaseAuth.getInstance().getCurrentUser();
        if(mUser == null) {
            return;
        }
        mUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        token = task.getResult().getToken();
                    } else {
                        Log.e("petjalog", task.getException().getMessage());
                    }
                });
    }

    public String getToken() {
        return token;
    }

    public static FirebaseManager getInstance() {
        if(instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
}

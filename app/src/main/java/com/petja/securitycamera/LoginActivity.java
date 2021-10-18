package com.petja.securitycamera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends Activity {

    Button login, google;
    EditText email, password, name, confirmPassword;
    ImageView showPassword, showConfirmPassword;
    TextView switchView, forgot;

    AuthView authView;

    FirebaseAuth auth;
    GoogleSignInClient mGoogleSignInClient;

    private static final int RC_SIGN_IN = 9001;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        showPassword = findViewById(R.id.showPassword);
        showConfirmPassword = findViewById(R.id.confirmPasswordShow);
        login = findViewById(R.id.login);
        google = findViewById(R.id.googleButton);
        switchView = findViewById(R.id.switch_view);
        forgot = findViewById(R.id.forgotPassword);

        authView = AuthView.LOGIN;

        switchView.setOnClickListener(view -> {
            if(authView == AuthView.LOGIN) {
                setView(AuthView.REGISTER);
            } else if(authView == AuthView.REGISTER) {
                setView(AuthView.LOGIN);
            } else if(authView == AuthView.FORGOT) {
                setView(AuthView.LOGIN);
            }
        });
        showPassword.setOnClickListener(view -> togglePasswordVisibility(password, showPassword));
        showConfirmPassword.setOnClickListener(view -> togglePasswordVisibility(confirmPassword, showConfirmPassword));

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if(currentUser != null){
            Log.d("petjalog", "already logged in");
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("890583654087-u6ajrfb8v6a76ejp05dh43k3dijarbvk.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        google.setOnClickListener(v -> onGoogleSignIn());
        login.setOnClickListener(v -> {
            if(authView == AuthView.LOGIN) {
                login();
            } else if(authView == AuthView.REGISTER){
                onRegister();
            } else if(authView == AuthView.FORGOT) {
                resetPassword();
            }
        });
        forgot.setOnClickListener(v -> {
            if(authView != AuthView.FORGOT) {
                setView(AuthView.FORGOT);
            }
        });
    }

    private void togglePasswordVisibility(EditText input, ImageView toggle) {
        if(input.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            toggle.setImageResource(R.drawable.ic_hide);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {
            toggle.setImageResource(R.drawable.ic_show);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        input.setSelection(input.length());
    }

    private void setView(AuthView authView) {
        this.authView = authView;
        if(authView == AuthView.LOGIN) {
            confirmPassword.setVisibility(View.GONE);
            showConfirmPassword.setVisibility(View.GONE);
            showPassword.setVisibility(View.VISIBLE);
            switchView.setText(R.string.register);
            login.setText(R.string.login);
            name.setVisibility(View.GONE);
            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topToBottom = R.id.name;
            lp.setMargins(0, 0, 0, 0);
            email.setLayoutParams(lp);
            google.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            forgot.setVisibility(View.VISIBLE);
            forgot.setText(R.string.forgotPassword);
        } else if(authView == AuthView.REGISTER) {
            password.setVisibility(View.VISIBLE);
            showPassword.setVisibility(View.VISIBLE);
            confirmPassword.setVisibility(View.VISIBLE);
            showConfirmPassword.setVisibility(View.VISIBLE);
            switchView.setText(getResources().getText(R.string.login));
            login.setText(R.string.register);
            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topToBottom = R.id.name;
            lp.setMargins(0,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()), 0, 0);
            email.setLayoutParams(lp);
            name.setVisibility(View.VISIBLE);
            forgot.setVisibility(View.GONE);
        } else if(authView == AuthView.FORGOT) {
            forgot.setText(R.string.please_enter_forgot);
            name.setVisibility(View.GONE);
            showPassword.setVisibility(View.GONE);
            confirmPassword.setVisibility(View.GONE);
            showConfirmPassword.setVisibility(View.GONE);
            password.setVisibility(View.GONE);
            google.setVisibility(View.GONE);
            login.setText(R.string.reset);
            switchView.setText(R.string.back);
            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topToBottom = R.id.name;
            lp.setMargins(0, 0, 0, 0);
            email.setLayoutParams(lp);

        }
    }

    private void login() {
        String email = this.email.length() != 0 ? this.email.getText().toString() : null;
        String password = this.password.length() != 0 ? this.password.getText().toString() : null;
        if(this.email.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please insert email", Toast.LENGTH_SHORT).show();
            return;
        }
        if(this.password.length() < 6) {
            Toast.makeText(getApplicationContext(), "Password too short", Toast.LENGTH_SHORT).show();
            return;
        }
        if(email == null || password == null) {
            return;
        }
        login.setEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("petjalog", "signInWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        String name = user.getDisplayName();
                        String email1 = user.getEmail();
                        Uri photoUrl = user.getPhotoUrl();
                        String uri = null;
                        if(photoUrl != null) {
                            uri = photoUrl.toString();
                        }
                        onSuccess();
                        Log.d("petjalog", name + ", " + email1 + ", " + uri);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("petjalog", "signInWithEmail:failure", task.getException());
                        Log.w("petjalog", task.getException().getMessage());
                        if(task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().equals("The email address is badly formatted.")) {
                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        } else if(task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().equals("There is no user record corresponding to this identifier. The user may have been deleted.")) {
                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        } else if(task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().equals("The password is invalid or the user does not have a password.")) {
                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        //updateUI(null);
                    }
                    login.setEnabled(true);
                });
    }

    private void onGoogleSignIn() {
        google.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void onSuccess() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("petjalog", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("petjalog", "Google sign in failed", e);
                Log.w("petjalog", "Google sign in failed "+  e.getMessage());
                Toast.makeText(getApplicationContext(), "Google login failed", Toast.LENGTH_SHORT).show();
                google.setEnabled(true);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("petjalog", "signInWithCredential:success");
                            FirebaseUser user = auth.getCurrentUser();
                            onSuccess();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("petjalog", "signInWithCredential:failure", task.getException());
                            Toast.makeText(getApplicationContext(), "Google login failed", Toast.LENGTH_SHORT).show();
                            //TODO
                        }
                        google.setEnabled(true);
                    }
                });
    }

    private void onRegister() {
        if (!confirmPassword.getText().toString().equals(password.getText().toString())) {
            Toast.makeText(getApplicationContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
        }
        login.setEnabled(false);
        auth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("petjalog", "createUserWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name.toString())
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateProfileTask -> {
                                        if (updateProfileTask.isSuccessful()) {
                                            Log.d("petjalog", "User profile updated.");
                                        }
                                        onSuccess();
                                        login.setEnabled(true);
                                    });
                        } else {
                            onSuccess();
                            login.setEnabled(true);
                        }
                    } else {
                        login.setEnabled(true);
                        // If sign in fails, display a message to the user.
                        Log.w("petjalog", "createUserWithEmail:failure", task.getException());
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Log.w("petjalog", "User already exists");
                            Toast.makeText(getApplicationContext(), "User already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w("petjalog", "Failed to create user");
                            Toast.makeText(getApplicationContext(), "Failed to create user", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void resetPassword() {
        if(email.length() == 0) {
            Toast.makeText(getApplicationContext(), "Invalid email", Toast.LENGTH_SHORT).show();
            return;
        }
        login.setEnabled(false);
        String email = this.email.getText().toString();
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String infoText = getText(R.string.passoword_reset_email_sent) + " " + email + ".";
                        forgot.setText(infoText);
                    } else {
                        Log.w("petjalog", task.getException());
                        if(task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().equals("The email address is badly formatted.")) {
                            Toast.makeText(getApplicationContext(), "Invalid email", Toast.LENGTH_SHORT).show();
                        } else if(task.getException() != null && task.getException().getMessage() != null && task.getException().getMessage().equals("There is no user record corresponding to this identifier. The user may have been deleted.")) {
                            Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            //TODO
                        }
                    }
                    login.setEnabled(true);
                });
    }

    enum AuthView {
        LOGIN,
        REGISTER,
        FORGOT
    }
}

package com.example.wanderlog;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {
    private static final String FIREBASE_PASSWORD_SYMBOLS = "^$*.[]{}()?\"!@#%&/\\,><':;|_~";
    private FirebaseAuth auth;
    private View registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        registerButton = findViewById(R.id.button);
    }

    public void RegisterBtn(View view) {
        EditText username = findViewById(R.id.username);
        EditText email = findViewById(R.id.email);
        EditText password1 = findViewById(R.id.password1);
        EditText password2 = findViewById(R.id.password2);

        String displayName = username.getText().toString().trim();
        String emailAddress = email.getText().toString().trim();
        String password = password1.getText().toString();

        if (displayName.length() < 3) {
            username.setError(getString(R.string.username_too_short));
            return;
        }
        if (!emailValidation(emailAddress)) {
            email.setError(getString(R.string.enter_valid_email));
            return;
        }
        if (!hasRequiredPasswordCharacters(password)) {
            password1.setError(getString(R.string.password_requirements));
            return;
        }
        if (!password.equals(password2.getText().toString())) {
            password2.setError(getString(R.string.passwords_do_not_match));
            return;
        }

        registerButton.setEnabled(false);
        auth.createUserWithEmailAndPassword(emailAddress, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        registerButton.setEnabled(true);
                        Toast.makeText(this, getString(R.string.registration_failed), Toast.LENGTH_LONG).show();
                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        openMain();
                        return;
                    }

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build();
                    user.updateProfile(profileUpdates).addOnCompleteListener(this, profileTask -> {
                        if (!profileTask.isSuccessful()) {
                            Toast.makeText(this, R.string.profile_name_not_saved, Toast.LENGTH_SHORT).show();
                        }
                        openMain();
                    });
                });
    }

    public void GoToLoginBtn(View view) {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private boolean emailValidation(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean hasRequiredPasswordCharacters(String password) {
        if (password.length() < 8) return false;

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        for (int i = 0; i < password.length(); i++) {
            char character = password.charAt(i);
            hasUppercase |= character >= 'A' && character <= 'Z';
            hasLowercase |= character >= 'a' && character <= 'z';
            hasDigit |= character >= '0' && character <= '9';
            hasSymbol |= FIREBASE_PASSWORD_SYMBOLS.indexOf(character) >= 0;
        }

        return hasUppercase && hasLowercase && hasDigit && hasSymbol;
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

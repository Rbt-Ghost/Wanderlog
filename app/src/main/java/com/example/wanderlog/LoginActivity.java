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

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private View loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        loginButton = findViewById(R.id.button);
    }

    public void LoginBtn(View view){

        EditText email = findViewById(R.id.email);
        EditText password = findViewById(R.id.password1);
        String emailAddress = email.getText().toString().trim();
        String passwordText = password.getText().toString();

        if (!emailValidation(emailAddress)) {
            email.setError(getString(R.string.enter_valid_email));
            return;
        }
        if (passwordText.isEmpty()) {
            password.setError(getString(R.string.enter_password));
            return;
        }

        loginButton.setEnabled(false);
        auth.signInWithEmailAndPassword(emailAddress, passwordText)
                .addOnCompleteListener(this, task -> {
                    loginButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, R.string.login_failed, Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void GoToRegisterBtn(View view){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean emailValidation(String email){
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}

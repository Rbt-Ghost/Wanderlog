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

public class RegisterActivity extends AppCompatActivity {

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
    }

    public void RegisterBtn(View view){

        EditText username = findViewById(R.id.username);
        EditText emial = findViewById(R.id.email);
        EditText password1 = findViewById(R.id.password1);
        EditText password2 = findViewById(R.id.password2);

        if( usernameValidation(username) && emailValidation(emial) && passValidation(password1, password2)){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(this,"Invalid data", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean usernameValidation(EditText username){
        return username.getText().toString().length() >= 3;
    }

    private  boolean emailValidation(EditText email){
        return email.getText().toString().length() >= 4 && email.getText().toString().contains("@") && email.getText().toString().contains(".");
    }

    private boolean passValidation(EditText password1, EditText password2){
        return password1.getText().toString().equals(password2.getText().toString()) && password1.getText().toString().length() >= 8;
    }
}
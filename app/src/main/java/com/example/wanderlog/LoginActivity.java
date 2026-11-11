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

public class LoginActivity extends AppCompatActivity {

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
    }

    public void LoginBtn(View view){

        EditText email = findViewById(R.id.email);
        EditText password1 = findViewById(R.id.password1);

        if( emailValidation(email) && passValidation(password1)){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(this,"Invalid data", Toast.LENGTH_SHORT).show();
        }
    }

    public void GoToRegisterBtn(View view){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private  boolean emailValidation(EditText email){
        return email.getText().toString().length() >= 4 && email.getText().toString().contains("@") && email.getText().toString().contains(".");
    }

    private boolean passValidation(EditText password1){
        return password1.getText().toString().length() >= 8;
    }
}
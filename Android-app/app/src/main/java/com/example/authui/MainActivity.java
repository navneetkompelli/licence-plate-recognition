package com.example.authui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private TextView userText;
    private TextView statusText;
    private EditText emailText;
    private EditText passwordText;
    private Button licence;

    private FirebaseAuth fbAuth;
    private FirebaseAuth.AuthStateListener authListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userText = (TextView) findViewById(R.id.userText);
        statusText = (TextView) findViewById(R.id.statusText);
        emailText = (EditText) findViewById(R.id.emailText);
        passwordText = (EditText) findViewById(R.id.passwordText);
        licence = (Button) findViewById(R.id.button5);
        userText.setText("");
        statusText.setText("Signed Out");

        fbAuth = FirebaseAuth.getInstance();

        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    userText.setText(user.getEmail());
                    statusText.setText("Signed In, Click on Licence");

                    licence.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            MainActivity.this.startActivity(new Intent(MainActivity.this, LicenceActivity.class));
                        }
                    });

                } else {
                    userText.setText("");
                    statusText.setText("Signed Out, Licence requires sign-in");

                    licence.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            notifyUser("Sign-in first");
                        }
                    });
                }
            }
        };


    }

    @Override
    public void onStart() {
        super.onStart();
        fbAuth.addAuthStateListener(authListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            fbAuth.removeAuthStateListener(authListener);
        }
    }

    private void notifyUser(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    public void createAccount(View view) {
        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if (email.length() == 0) {
            emailText.setError("Enter an email address");
            return;
        }

        if (password.length() < 6) {
            passwordText.setError("Password must be at least 6 characters");
            return;
        }

        fbAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                if (!task.isSuccessful()) {
                                    notifyUser("Account creation failed");
                                }
                            }
                        });

    }

    public void signIn(View view) {

        String email = emailText.getText().toString();
        String password = passwordText.getText().toString();

        if (email.length() == 0) {
            emailText.setError("Enter an email address");
            return;
        }

        if (password.length() < 6) {
            passwordText.setError("Password must be at least 6 characters");
            return;
        }

        fbAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this,
                        new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull
                                                           Task<AuthResult> task) {

                                if (!task.isSuccessful()) {
                                    notifyUser("Authentication failed");
                                }
                            }
                        });

    }

    public void signOut(View view) {
        fbAuth.signOut();
    }

    public void resetPassword(View view) {

        String email = emailText.getText().toString();

        if (email.length() == 0) {
            emailText.setError("Enter an email address");
            return;
        }

        fbAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            notifyUser("Reset email sent");
                        }
                    }
                });
    }

}
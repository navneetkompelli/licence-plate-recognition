package com.example.authui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class LicenceActivity extends AppCompatActivity {

    private static final String TAG = "LicenceActivity";
    private EditText et_licencenum;
    private TextView tv_token;
    private Button btn_submit;
    private Button btn_delete;

    private static FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_licence);

        et_licencenum = (EditText) findViewById(R.id.et_licencenum);
        tv_token = (TextView) findViewById(R.id.tv_token);
        btn_submit = (Button) findViewById(R.id.btn_submit);
        btn_delete = (Button) findViewById(R.id.btn_delete);
        database = FirebaseDatabase.getInstance();
        dbRef = database.getReference("/tokensplates");

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        String fcm_token = task.getResult();
                        //tv_token.setText(fcm_token);

                        btn_submit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (TextUtils.isEmpty(et_licencenum.getText().toString())){
                                    notifyUser("Submit error - Enter plate number");
                                }
                                else {
                                    dbRef.child(et_licencenum.getText().toString()).setValue(fcm_token, completionListener);
                                    notifyUser(et_licencenum.getText().toString() + " is now registered");
                                }
                            }
                        });

                        btn_delete.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (TextUtils.isEmpty(et_licencenum.getText().toString())){
                                    notifyUser("Delete error - Enter plate number");
                                }
                                else {
                                    dbRef.child(et_licencenum.getText().toString()).removeValue();
                                    notifyUser(et_licencenum.getText().toString() + " is now de-registered");
                                }
                            }
                        });
                    }
                });

    }

    DatabaseReference.CompletionListener completionListener =
            new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError,
                                       DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        notifyUser(databaseError.getMessage());
                    }
                }
            };

    private void notifyUser(String message) {
        Toast.makeText(LicenceActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
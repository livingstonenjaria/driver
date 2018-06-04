package ke.co.struct.chauffeurdriver;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText email, password;
    private String str_email, str_password;
    private Button  login;
    private TextView txt_register, txt_forgot;
    android.app.Dialog  loginDialog;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    private String error;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/PTMono.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
        setContentView(R.layout.activity_login);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        login = findViewById(R.id.btn_login);
        txt_forgot = findViewById(R.id.txt_forgot);
        txt_register = findViewById(R.id.txt_register);

        txt_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginDialog  = new SpotsDialog(LoginActivity.this);
                login.setEnabled(false);
                loginDialog.setCancelable(false);
                loginDialog.show();
                str_email = email.getText().toString();
                str_password = password.getText().toString();
                if (!str_password.isEmpty() && !str_email.isEmpty()){
                    auth.signInWithEmailAndPassword(str_email, str_password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            login.setEnabled(true);
                            loginDialog.dismiss();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            login.setEnabled(true);
                            loginDialog.dismiss();
                            Log.e(TAG, "onFailure: " +e.getMessage());
                            error = e.getMessage();
                            erroralert(error);
                        }
                    });
                }
                else{
                    Toast.makeText(LoginActivity.this, "Please fill all the required fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void erroralert(String message){
        Log.d(TAG, "erroralert: Dialog");
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Whoa! there's an error")
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_error);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

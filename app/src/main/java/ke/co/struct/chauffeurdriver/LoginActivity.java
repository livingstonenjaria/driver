package ke.co.struct.chauffeurdriver;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.w3c.dom.Text;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private EditText email, password;
    private String str_email, str_password;
    private Button  login;
    private TextView txt_register, txt_forgot;
    CoordinatorLayout loginRoot;
    android.app.Dialog  loginDialog, waitingDialog;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    private String error ;
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
        loginRoot = findViewById(R.id.loginRoot);
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
        txt_forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogForgotPwd();
            }
        });
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginDialog  = new SpotsDialog(LoginActivity.this, getResources().getString(R.string.logging_in));
                login.setEnabled(false);
                loginDialog.setCancelable(false);
                loginDialog.show();
                str_email = email.getText().toString().trim();
                str_password = password.getText().toString().trim();
                if (!TextUtils.isEmpty(str_password) && !TextUtils.isEmpty(str_email)){
                    auth.signInWithEmailAndPassword(str_email, str_password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            login.setEnabled(true);
                            loginDialog.dismiss();
                            startActivity(new Intent(LoginActivity.this, DriverHomeActivity.class));
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
                    error = getResources().getString(R.string.please_fill_all);
                    erroralert(error);
                }
            }
        });
    }

    private void showDialogForgotPwd() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this);
        alertDialog.setTitle(R.string.forgot_pwd);
        alertDialog.setMessage(R.string.please_enter_email);
        LayoutInflater inflater = LayoutInflater.from(LoginActivity.this);
        View forgot_pwd = inflater.inflate(R.layout.layout_forgot,null);
        final EditText forgot_email = forgot_pwd.findViewById(R.id.forgot_email);
        alertDialog.setView(forgot_pwd);
        alertDialog.setPositiveButton(R.string.reset, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                waitingDialog = new SpotsDialog(LoginActivity.this, getResources().getString(R.string.resetting_pwd));
                waitingDialog.setCancelable(false);
                waitingDialog.show();
                String reset_email = forgot_email.getText().toString().trim();
                if (!TextUtils.isEmpty(reset_email)){
                auth.sendPasswordResetEmail(reset_email).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        dialog.dismiss();
                        waitingDialog.dismiss();
                        Snackbar.make(loginRoot,getResources().getString(R.string.reset_link), Snackbar.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        waitingDialog.dismiss();
                        erroralert(e.getMessage());
                    }
                });
                }else{
                    waitingDialog.dismiss();
                     error = getResources().getString(R.string.please_enter_email);
                    erroralert(error);
                }
            }
        });
        alertDialog.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
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

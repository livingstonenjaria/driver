package ke.co.struct.chauffeurdriver;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import dmax.dialog.SpotsDialog;
import ke.co.struct.chauffeurdriver.model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private EditText reg_email, reg_password, reg_name,  phone;
    private String  str_email, str_password, str_name, str_phone;
    private Button btn_register;
    private TextView txt_login;
    private  String uid;
    private String weakpass;
    private String malformedemail;
    private String existingemail ;
    private static final  int num = 0;
    android.app.Dialog registerDialog;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseDatabase db = FirebaseDatabase.getInstance();
    DatabaseReference users;
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
        setContentView(R.layout.activity_register);
        reg_email = findViewById(R.id.reg_email);
        reg_password = findViewById(R.id.reg_password);
        reg_name = findViewById(R.id.reg_name);
        phone = findViewById(R.id.phone);
        btn_register = findViewById(R.id.btn_register);
        txt_login = findViewById(R.id.txt_login);
        users = db.getReference("Users").child("Drivers");



        txt_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerDialog  = new SpotsDialog(RegisterActivity.this);
                btn_register.setEnabled(false);
                registerDialog.setCancelable(false);
                registerDialog.show();
                str_email = reg_email.getText().toString();
                str_name = reg_name.getText().toString();
                str_password = reg_password.getText().toString();
                str_phone = phone.getText().toString();
                if(!str_password.isEmpty() && !str_name.isEmpty() && !str_email.isEmpty() && !str_phone.isEmpty() ) {
                    auth.createUserWithEmailAndPassword(str_email, str_password)
                            .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                @Override
                                public void onSuccess(AuthResult authResult) {
                                    btn_register.setEnabled(true);
                                    registerDialog.dismiss();
                                    uid = authResult.getUser().getUid();
                                    User user = new User(str_name, str_email, str_password, str_phone);
                                    users.child(uid).setValue(user);
                                    db.getReference().child("ratingSum").child(uid).child("sum").setValue(num);
                                    db.getReference().child("numofratings").child(uid).child("rated").setValue(num);
                                    db.getReference().child("driverEarnings").child(uid).child("earnings").setValue(num);
                                    db.getReference().child("driverTrips").child(uid).child("trips").setValue(num);
                                    db.getReference().child("numoffivestarratings").child(uid).child("ratings").setValue(num);
                                }
                            })
                           .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                            } else {
                                btn_register.setEnabled(true);
                                registerDialog.dismiss();
                                try {
                                    throw task.getException();
                                } catch(FirebaseAuthWeakPasswordException e) {
                                    weakpass = getResources().getString(R.string.weakpass);
                                    erroralert(weakpass);
                                    Log.e(TAG, "onComplete:  " + e.getMessage());
                                } catch(FirebaseAuthInvalidCredentialsException e) {
                                    malformedemail = getResources().getString(R.string.malformedemail);
                                    erroralert(malformedemail);
                                    Log.e(TAG, "onComplete:  " + e.getMessage());
                                } catch(FirebaseAuthUserCollisionException e) {
                                    existingemail = getResources().getString(R.string.existingemail);
                                    erroralert(existingemail);
                                    Log.e(TAG, "onComplete:  " + e.getMessage());
                                } catch(Exception e) {
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        }
                    });
                } else{
                    Toast.makeText(RegisterActivity.this, "Please fill all the required fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void erroralert(String message){
        Log.d(TAG, "erroralert: Dialog");
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(RegisterActivity.this);
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

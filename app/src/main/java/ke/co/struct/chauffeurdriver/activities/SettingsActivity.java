package ke.co.struct.chauffeurdriver.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import ke.co.struct.chauffeurdriver.R;
import ke.co.struct.chauffeurdriver.remote.Common;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";
    private MaterialEditText  fullnames,phone,id,email;
    private CircleImageView addphoto, addcar;
    android.app.Dialog  waitingDialog;
    private String error, userid;
    CoordinatorLayout settingsRoot;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private AlertDialog imagedialog;
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
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        settingsRoot = findViewById(R.id.settingsRoot);
        fullnames = findViewById(R.id.fullnames);
        email = findViewById(R.id.email);
        id = findViewById(R.id.nationalid);
        phone = findViewById(R.id.phone);
        addcar = findViewById(R.id.addcar);
        addphoto = findViewById(R.id.profilepic);
        userid = Common.auth.getCurrentUser().getUid();
        setValues();
        addphoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseProfile();
            }
        });
        addcar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCardialog();
            }
        });
        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phoneDialog();
            }
        });
        id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                idDialog();
            }
        });
        fullnames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullnamesDialog();
            }
        });

    }

    private void setValues() {
        if (!TextUtils.isEmpty(Common.current_driver.getName())) {
            fullnames.setText(Common.current_driver.getName());
        }
        if (!TextUtils.isEmpty(Common.current_driver.getPhone())) {
            phone.setText(Common.current_driver.getPhone());
        }
        if (!TextUtils.isEmpty(Common.current_driver.getNationalID())) {
            id.setText(Common.current_driver.getNationalID());
        }
        if (!TextUtils.isEmpty(Common.auth.getCurrentUser().getEmail())) {
            email.setText(Common.auth.getCurrentUser().getEmail());
        }
        if (!TextUtils.isEmpty(Common.current_driver.getCarimg())) {
            Picasso.get()
                    .load(Common.current_driver.getCarimg())
                    .resize(200,200)
                    .centerCrop()
                    .into(addcar);
        }
        if (!TextUtils.isEmpty(Common.current_driver.getProfileImageUrl())) {
            Picasso.get()
                    .load(Common.current_driver.getProfileImageUrl())
                    .resize(200,200)
                    .centerCrop()
                    .into(addphoto);
        }
    }

    private void fullnamesDialog() {
        android.support.v7.app.AlertDialog.Builder alertDialog = new android.support.v7.app.AlertDialog.Builder(SettingsActivity.this);
        alertDialog.setTitle(R.string.update_name);
        alertDialog.setMessage(R.string.please_enter_name);
        LayoutInflater inflater = LayoutInflater.from(SettingsActivity.this);
        View layout_name = inflater.inflate(R.layout.layout_fullname,null);
        final MaterialEditText update_name = layout_name.findViewById(R.id.edt_fullname);
        alertDialog.setView(layout_name);
        alertDialog.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                waitingDialog = new SpotsDialog(SettingsActivity.this, getString(R.string.updating_details));
                waitingDialog.setCancelable(false);
                waitingDialog.show();
                String newname = update_name.getText().toString().trim();
                if (!TextUtils.isEmpty(newname)){

                }else{
                    waitingDialog.dismiss();
                    error = getString(R.string.please_enter_name);
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

    private void idDialog() {
        android.support.v7.app.AlertDialog.Builder alertDialog = new android.support.v7.app.AlertDialog.Builder(SettingsActivity.this);
        alertDialog.setTitle(R.string.update_id);
        alertDialog.setMessage(R.string.please_enter_id);
        LayoutInflater inflater = LayoutInflater.from(SettingsActivity.this);
        View layout_id = inflater.inflate(R.layout.layout_nationalid,null);
        final MaterialEditText update_id = layout_id.findViewById(R.id.edt_nationalid);
        alertDialog.setView(layout_id);
        alertDialog.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                waitingDialog = new SpotsDialog(SettingsActivity.this, getString(R.string.updating_details));
                waitingDialog.setCancelable(false);
                waitingDialog.show();
                String newid = update_id.getText().toString().trim();
                if (!TextUtils.isEmpty(newid)){

                }else{
                    waitingDialog.dismiss();
                    error = getString(R.string.please_enter_id);
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

    private void phoneDialog() {
        android.support.v7.app.AlertDialog.Builder alertDialog = new android.support.v7.app.AlertDialog.Builder(SettingsActivity.this);
        alertDialog.setTitle(R.string.update_phone);
        alertDialog.setMessage(R.string.please_enter_phone);
        LayoutInflater inflater = LayoutInflater.from(SettingsActivity.this);
        View layout_phone = inflater.inflate(R.layout.layout_phone,null);
        final MaterialEditText update_phone = layout_phone.findViewById(R.id.edt_phone);
        alertDialog.setView(layout_phone);
        alertDialog.setPositiveButton(R.string.update, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                waitingDialog = new SpotsDialog(SettingsActivity.this, getString(R.string.updating_details));
                waitingDialog.setCancelable(false);
                waitingDialog.show();
                String newphone = update_phone.getText().toString().trim();
                if (!TextUtils.isEmpty(newphone)){

                }else{
                    waitingDialog.dismiss();
                    error = getString(R.string.please_enter_phone);
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

    private void addCardialog() {
        android.support.v7.app.AlertDialog.Builder builder;
        builder = new android.support.v7.app.AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle(R.string.info_header)
                .setMessage(R.string.car_info)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.info);
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void chooseProfile() {
        final AlertDialog.Builder  alertdialog = new AlertDialog.Builder(SettingsActivity.this);
        alertdialog.setTitle(R.string.update_profile_pic);
        alertdialog.setMessage(R.string.please_select_profile);

        LayoutInflater inflater = this.getLayoutInflater();
        View layout_profile = inflater.inflate(R.layout.layout_profile, null);
        Button upload = layout_profile.findViewById(R.id.uploadpic);
        Button cancel = layout_profile.findViewById(R.id.cancel_btn);
        alertdialog.setView(layout_profile);
        imagedialog  = alertdialog.create();
        imagedialog.show();
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
                imagedialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagedialog.dismiss();
            }
        });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_pic)), Common.PICK_IMAGE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Common.PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            Uri saveUri = data.getData();
            if (saveUri != null){
                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage(getString(R.string.uploading));
                progressDialog.setCancelable(false);
                progressDialog.show();
                String imageName = UUID.randomUUID().toString();
                final StorageReference imageFolder = storageReference.child("profile_image").child("drivers").child(userid).child(imageName);
                imageFolder.putFile(saveUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageFolder.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Map<String,Object> avatarUpdate = new HashMap<>();
                                        avatarUpdate.put("ProfileImageUrl", uri.toString());
                                        DatabaseReference driverprofile = Common.database.getReference().child("Users").child("Drivers").child(userid);
                                        driverprofile.updateChildren(avatarUpdate)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()){
                                                    progressDialog.dismiss();
                                                    Snackbar.make(settingsRoot, R.string.image_upload_success,Snackbar.LENGTH_LONG).show();
                                                }
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                                error = e.getMessage();
                                                erroralert(error);
                                            }
                                        });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                error = e.getMessage();
                                erroralert(error);

                            }
                        });
                    }
                })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress = (100.0*taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                                progressDialog.setMessage("Uploaded "+ String.format("%.2f", progress) + "%");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        error = e.getMessage();
                        erroralert(error);
                    }
                });
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // API 5+ solution
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void erroralert(String message){ Log.d(TAG, "erroralert: Dialog");
        android.support.v7.app.AlertDialog.Builder builder;
        builder = new android.support.v7.app.AlertDialog.Builder(SettingsActivity.this);
        builder.setTitle(R.string.error_header)
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(R.drawable.ic_error);
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();
    }
}

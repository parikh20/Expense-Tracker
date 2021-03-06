package team16.cs307.expensetracker;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import android.provider.MediaStore;

import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.String.*;

import com.google.android.gms.auth.api.Auth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ImageActivity extends AppCompatActivity {
    private Button choose,upload,camera;
    private TextView tag;
    private ImageView imageview;
    private Uri filePath;
    ProgressDialog pd;
    private String timeStamp;
    private Date date;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String textTag= "";




    private final int PICK_IMAGE_REQUEST = 1;
    private final int CAMERA_REQUEST = 2;
    private final int PERMISSION_CODE = 1000;
    FirebaseStorage storage;
    StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        mAuth = FirebaseAuth.getInstance();
        //Initialize
        db = FirebaseFirestore.getInstance();
        choose = (Button) findViewById(R.id.ImageActivity_Choose);
        upload = (Button) findViewById(R.id.ImageActivity_Upload);
        camera = (Button) findViewById(R.id.ImageActivity_Camera);
        imageview = (ImageView) findViewById(R.id.ImageActivity_imgView);
        tag = (TextView) findViewById(R.id.ImageActivity_Tag);
        pd = new ProgressDialog(this);
        pd.setMessage("Uploading...");
        storage  = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        tag.setText("Tag: None");


        //camera need




        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){

                        String[] permission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission,PERMISSION_CODE);
                    }else{
                        imageCamera();
                    }
                }else{
                    imageCamera();
                }

            }
        });
        tag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeTag();
            }
        });
    }



        //change tag
        private void changeTag(){
            final EditText edittext = new EditText(ImageActivity.this);
            edittext.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            new AlertDialog.Builder( ImageActivity.this )
                    .setTitle( "Tag" )
                    .setMessage( "Change your tag (Max Length: 6)" )
                    .setView(edittext)
                    .setPositiveButton( "Change", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            textTag = edittext.getText().toString();
                            if(textTag.length()==0){textTag="None";}
                            tag.setText("Tag:"+textTag);

                        }
                    })
                    .setNegativeButton( "Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    } )
                    .show();


        }
        //choose image from gallery
        private void chooseImage(){
             //choose images from gallery
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            startActivityForResult(intent,PICK_IMAGE_REQUEST);
        }

        //camera
        private void imageCamera(){
                //set filepath and camera setting
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE,"New Picture");
                values.put(MediaStore.Images.Media.DESCRIPTION,"Camera");
                filePath= getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,filePath);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);



                //}


        }
        @Override
        protected void onActivityResult(int requestCode,int resultCode,Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK ) {
                /*if(data.getClipData()!=null){
                    Toast.makeText(ImageActivity.this,"multiple",Toast.LENGTH_SHORT).show();
                }else */
                if(data.getData() != null){
                    filePath = data.getData();
                    try {
                        Picasso.get().load(filePath).fit().centerCrop().into(imageview);
                    } catch (Exception e) {
                        e.printStackTrace();

                    }

                }

            }
            else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK ){

                try {
                    Picasso.get().load(filePath).fit().centerCrop().into(imageview);
                } catch (Exception e) {
                    e.printStackTrace();

                }


            }
        }

        private void uploadImage(){
                if(filePath!=null){
                    pd.show();
                    timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    final StorageReference ref = storageReference.child(mAuth.getUid()+"/"+timeStamp);
                    final UploadTask uploadTask = ref.putFile(filePath);

                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Task <Uri> downloadUriTask = ref.getDownloadUrl();
                            while (!downloadUriTask.isSuccessful());
                            Uri downloadUri = downloadUriTask.getResult();
                            if (downloadUri !=null) {
                                String imgurl = downloadUri.toString().substring(downloadUri.toString().lastIndexOf('/') + 1);;
                                Map<String, Object> map = new HashMap<>();
                                map.put("imgurl", imgurl);
                                map.put("date",timeStamp);
                                if (textTag.length()==0){
                                    map.put("tag","None");
                                } else{
                                    map.put("tag",textTag);
                                }
                                db.collection("users").document(mAuth.getUid()).collection("images").document(imgurl).set(map);

                            }

                            pd.dismiss();
                            Toast.makeText(ImageActivity.this, "Uploaded" , Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            pd.dismiss();
                            Toast.makeText(ImageActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    });}
                    else{
                    Toast.makeText(ImageActivity.this, "Please select an image", Toast.LENGTH_SHORT).show();
                }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case PERMISSION_CODE:{
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    imageCamera();
                }else{
                    Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed(){
        onSupportNavigateUp();

    }


}




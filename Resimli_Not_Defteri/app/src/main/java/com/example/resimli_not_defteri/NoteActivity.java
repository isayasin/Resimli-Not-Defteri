package com.example.resimli_not_defteri;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.example.resimli_not_defteri.databinding.ActivityNoteBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;

public class NoteActivity extends AppCompatActivity {
    private ActivityNoteBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoteBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.equals("new")){
            binding.nameTxt.setText("");
            binding.artistTxt.setText("");
            binding.yearTxt.setText("");
            binding.buttonsave.setVisibility(View.VISIBLE);
            binding.buttonUpdate.setVisibility(View.INVISIBLE);
            binding.buttonDelete.setVisibility(View.INVISIBLE);
            binding.imageView.setImageResource(R.drawable.selectimage);
        }else {
            int artId = intent.getIntExtra("artId",1);
            binding.buttonsave.setVisibility(View.INVISIBLE);

            try {
                Cursor cursor = database.rawQuery("SELECT * FROM Arts WHERE id = ?",new String[] {String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artName");
                int painterNameIx = cursor.getColumnIndex("painterName");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.nameTxt.setText(cursor.getString(artNameIx));
                    binding.artistTxt.setText(cursor.getString(painterNameIx));
                    binding.yearTxt.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }

                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }



    public void save(View view){
        String name = binding.nameTxt.getText().toString();
        String artistName = binding.artistTxt.getText().toString();
        String year = binding.yearTxt.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {
            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS Arts(id INTEGER PRIMARY KEY, artName VARCHAR, painterName VARCHAR,year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO Arts(artName, painterName, year, image) VALUES(?, ?, ?, ?)";

            //üstte verilen yani girilen değerleri dönüştürerek sqlite a kaydeder (stringi sqlite komutuna dönüştürür)
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);

            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(NoteActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void guncelle(View view){
        String artName = binding.nameTxt.getText().toString();
        String painterName = binding.artistTxt.getText().toString();
        String year = binding.yearTxt.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] image = outputStream.toByteArray();

        try {
            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS Arts (id INTEGER PRIMARY KEY, artName VARCHAR, painterName VARCHAR,year VARCHAR, image BLOB)");

            //String sqlString2 = "INSERT INTO arts(artname, paintername, year, image) VALUES(?, ?, ?, ?)";
            String sqlString ="UPDATE Arts SET artName=?, painterName=?, year=?, image=?  WHERE artName=?";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);

            sqLiteStatement.bindString(1,artName);
            sqLiteStatement.bindString(2,painterName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,image);
            sqLiteStatement.bindString(5,artName);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(NoteActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    public void sil(View view){
        String name = binding.nameTxt.getText().toString();

        try {
            database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS Arts(id INTEGER PRIMARY KEY, artName VARCHAR, painterName VARCHAR,year VARCHAR, image BLOB)");


            String sqlString ="DELETE FROM Arts WHERE artName=?" ;
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(NoteActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }



    public void selectImage(View view){

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            }else{
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }else{
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }
    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width/ (float) height;

        if (bitmapRatio>1){
            width = maximumSize;
            height = (int) (width/bitmapRatio);
        }else{
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return image.createScaledBitmap(image,width,height,true);
    }

    private void registerLauncher(){

        activityResultLauncher =registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if(result.getResultCode() == RESULT_OK){
                    Intent intentFromResult = result.getData();
                    if(intentFromResult != null){
                        Uri imageData = intentFromResult.getData();
                        //binding.imageView.setImageURI(imageData);

                        try {
                            if (Build.VERSION.SDK_INT >= 28){
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            } else {
                                selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(),imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result){
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                } else {
                    Toast.makeText(NoteActivity.this,"Permission needed!",Toast.LENGTH_LONG).show();
                }
            }
        });

    }

}
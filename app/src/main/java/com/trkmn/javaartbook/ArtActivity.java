package com.trkmn.javaartbook;

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

import com.google.android.material.snackbar.Snackbar;
import com.trkmn.javaartbook.databinding.ActivityArtBinding;

import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;

    /*Aktivite sonuç başlatıcı ->
      Biz galeriye gidip ordan resim seçip onu ele almak mı istiyoruz
      Biz bir izin isteyip o izin verildiğinde ne oalcağını yazmak mı istiyoruz
      Hepsinde ActivityResultLauncher kullanacağız
     */
    ActivityResultLauncher<Intent> activityResultLauncher; //Galeriye gitmek için
    ActivityResultLauncher<String> permissionLauncher; //izinlerde kullanacağız
    //Bunları onCreate altında register etmemiz gerekiyor

    Bitmap selectedImage;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        registerLauncher();

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if (info.equals("new")){
            //new art
            binding.nameText.setText("");
            binding.painterText.setText("");
            binding.yearText.setText("");
            binding.imageView.setImageResource(R.drawable.selectimage);
            binding.button.setVisibility(View.VISIBLE);
        }else{
            int artId = intent.getIntExtra("artId", 0);
            binding.button.setVisibility(View.INVISIBLE);

            try {
                //artId, ? yerine geçecek
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[]{String.valueOf(artId)});
                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.painterText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,bytes.length); //byte[] to bitmap
                    binding.imageView.setImageBitmap(bitmap);
                }

                cursor.close();

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }



    public void save(View view){
        String artName = binding.nameText.getText().toString();
        String painterName = binding.painterText.getText().toString();
        String year = binding.yearText.getText().toString();

        //SQL'e kaydedebilmek için resmin boyutunu küçültüyoruz
        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        //Veri tabanına koymak için 0 ve 1'lerden oluşan veri dizisine yani byte dizisine çeviriyoruz
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] byteArray =  outputStream.toByteArray();

        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");
            String sqlString = "INSERT INTO arts(artname, paintername, year, image) VALUES(?, ?, ?, ?)";
            //Veri tabanımızda sql çalıştırmaya çalıştırırken bağlama (binding) işlemlerini kolay yapmamızı sağlayan yapı
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,artName); //İstediği index, ?'nin indexi. (Burada indexler 1'den başlıyor)
            sqLiteStatement.bindString(2,painterName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }

        Intent intent = new Intent(ArtActivity.this, MainActivity.class);
        //Bundan önceki ve içinde bulunduğumuz aktivite dahil bütün çalışan aktiviteleri kapatır ve gideceğimiz aktiviteyi açar
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize){
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1){
            //landscape image
            width = maximumSize;
            height = (int) (width / bitmapRatio);

        }else{
            //portrait image
            height = maximumSize;
            width = (int)(height * bitmapRatio);

        }

        //Parametreler (source, width, height, filter)
        return Bitmap.createScaledBitmap(image,width,height,true);
    }

    public void selectImage(View view){
        //Android Tiramisu API 33Ten sonra gelen READ_MEDIA_IMAGES izni geldiği için bu kontrolü eklememiz gerekiyor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            //Android 33+ -> READ_MEDIA_IMAGES
            //İzinleri kontrol etmek
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)){
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    }).show();
                }else{
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }

            }else{
                //gallery
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);
            }
        }else{
            //Android 32- -> READ_EXTERNAL_STORAGE
            //İzinleri kontrol etmek
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }).show();
                }else{
                    //request permission
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                }

            }else{
                //gallery
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activityResultLauncher.launch(intentToGallery);
            }
        }
    }

    private void registerLauncher(){
        //Galeriye gitmek
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK){//Kullanıcı bir şey seçti
                    Intent intentFromResult = result.getData();
                    if (intentFromResult != null){
                        //Bu getData kullanıcının seçtiği görselin nerede kayıtlı olduğunu veriyor
                        Uri imageData =  intentFromResult.getData();

                        //Bize kullanıcının resminin verisi lazım olduğu için bitmaple işlemi yapıyoruz. Çünkü o veriiyi veri tabanına kaydedeceğiz
                        //binding.imageView.setImageURI(imageData);

                        //Görseli Bitmap'e çevirme
                        try {
                            if (Build.VERSION.SDK_INT >= 28){
                                //Bu işlem sadece API 28 ve üzerinde çalışıyor. Bu yüzden bu kontrolü yapıyoruz
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(),imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            }else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        //İzni isteme
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result){
                    //Permission granted
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    //gallery
                    activityResultLauncher.launch(intentToGallery);

                }else {
                    //permission denied
                    Toast.makeText(ArtActivity.this, "Permission needed!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
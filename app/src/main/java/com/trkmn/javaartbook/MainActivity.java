package com.trkmn.javaartbook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.trkmn.javaartbook.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    ArrayList<Art> artArrayList;

    ArtAdapter artAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ArtActivity.class);
                intent.putExtra("info","new");
                startActivity(intent);
            }
        });

        artArrayList = new ArrayList<>();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        artAdapter = new ArtAdapter(artArrayList);
        binding.recyclerView.setAdapter(artAdapter);

        getData();
    }

    private void getData(){

        try {
            SQLiteDatabase sqLiteDatabase = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM arts", null);
            int nameIx = cursor.getColumnIndex("artname");
            int idIx = cursor.getColumnIndex("id");

            while (cursor.moveToNext()){
                String name = cursor.getString(nameIx);
                int id = cursor.getInt(idIx);
                Art art = new Art(name, id);
                artArrayList.add(art);
            }

            //Yeni veri geldiğinde adapter güncellenecek
            artAdapter.notifyDataSetChanged();

            cursor.close();

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
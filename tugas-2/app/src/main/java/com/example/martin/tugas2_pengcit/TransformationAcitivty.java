package com.example.martin.tugas2_pengcit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.ToggleButton;

public class TransformationAcitivty extends AppCompatActivity {
    private Bitmap rawBitmap;
    private ImageView transformImageView;
    private ImageProcessor imageProcessor;
    private String[] algoChoice;
    private String algoSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transformation);
        transformImageView = (ImageView) findViewById(R.id.transformedImageView);
        imageProcessor = new ImageProcessor();

        Intent intent = getIntent();
        rawBitmap = (Bitmap) intent.getParcelableExtra("Image");
        transformImageView.setImageBitmap(rawBitmap);

        // setup spinner
        algoSelected = "Original";
        Spinner algoSpinner = this.findViewById(R.id.algorithmSpinner);

        algoChoice = new String[] {"Original", "ALU", "Linear Stretching"};
        final ArrayAdapter<String> algorithmList = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, algoChoice);
        algoSpinner.setAdapter(algorithmList);
        algoSpinner.setSelection(0);
        algoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                algoSelected = algoChoice[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // setup button
        Button transformButton = findViewById(R.id.transformButton);
        transformButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                transformImageView.setImageBitmap(transformBitmap(rawBitmap, algoSelected));
            }
        });
    }

    private Bitmap transformBitmap(Bitmap bitmap, String algoName) {
        // do transformation
        if (algoName.equals("Original")) {
            return bitmap;
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Log.d("debug", "w : " + Integer.toString(w));
        Log.d("debug", "h : " + Integer.toString(h));

        int[][] r = new int[h][w];
        int[][] g = new int[h][w];
        int[][] b = new int[h][w];

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int colour = bitmap.getPixel(j, i);
                r[i][j] = Color.red(colour);
                g[i][j] = Color.green(colour);
                b[i][j] = Color.blue(colour);
            }
        }

        if (algoName.equals("ALU")) {
            r = imageProcessor.transformCumulative(r, h, w);
            g = imageProcessor.transformCumulative(g, h, w);
            b = imageProcessor.transformCumulative(b, h, w);
            Log.d("debug", "algo used: ALU");
        } else if( algoName.equals("Linear Stretching")) {
            r = imageProcessor.linearStretching(r, h, w);
            g = imageProcessor.linearStretching(g, h, w);
            b = imageProcessor.linearStretching(b, h, w);
            Log.d("debug", "algo used Linear Stretching");
        }

        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        Bitmap transformed_bitmap = Bitmap.createBitmap(w, h, config);

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                int colour = Color.rgb(r[i][j], g[i][j], b[i][j]);
                transformed_bitmap.setPixel(j, i, colour);
            }
        }

        return transformed_bitmap;
    }
}

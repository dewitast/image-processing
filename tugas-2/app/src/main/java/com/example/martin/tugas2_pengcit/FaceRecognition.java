package com.example.martin.tugas2_pengcit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.lang.reflect.Array;
import java.util.ArrayList;

import uk.co.senab.photoview.PhotoViewAttacher;

public class FaceRecognition extends AppCompatActivity {

    private Bitmap rawBitmap;
    private Bitmap curBitmap;
    private ImageView faceImageView;
    private Button searchButton;
    private Button backButton;
    private FaceDetector faceDetector;
    private int[][] a, r, g, b, gr, bw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        faceDetector = new FaceDetector();

        // read data from
        Intent intent = getIntent();
        rawBitmap = intent.getParcelableExtra("Image");
        curBitmap = rawBitmap;

        int w = rawBitmap.getWidth();
        int h = rawBitmap.getHeight();

        a = new int[w][h];
        r = new int[w][h];
        g = new int[w][h];
        b = new int[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int colour = rawBitmap.getPixel(i, j);
                a[i][j] = Color.alpha(colour);
                r[i][j] = Color.red(colour);
                g[i][j] = Color.green(colour);
                b[i][j] = Color.blue(colour);
            }
        }

        // initiate android element variable
        faceImageView = findViewById(R.id.faceImageView);
        faceImageView.setImageBitmap(rawBitmap);

        PhotoViewAttacher photoViewAttacher = new PhotoViewAttacher(faceImageView);
        photoViewAttacher.update();

        // setting search button
        searchButton = findViewById(R.id.faceSearchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFace();
                faceImageView.setImageBitmap(curBitmap);
            }
        });

        // setup back button
        backButton = findViewById(R.id.faceBackButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resIntent = new Intent();
                resIntent.putExtra("Image", curBitmap);
                setResult(Activity.RESULT_OK, resIntent);
                finish();
            }
        });
    }

    void createRectangle(int minx, int maxx, int miny, int maxy, int rr, int gg, int bb) {
        for (int i = minx; i <= maxx; i++) {
            r[i][miny] = r[i][maxy] = rr;
            g[i][miny] = g[i][maxy] = gg;
            b[i][miny] = b[i][maxy] = bb;
            gr[i][miny] = gr[i][maxy] = 128;
            bw[i][miny] = bw[i][maxy] = 128;
        }
        int midx = (minx + maxx) / 2;
        for (int i = miny; i <= maxy; i++) {
            r[minx][i] = r[maxx][i] = rr;
            g[minx][i] = g[maxx][i] = gg;
            b[minx][i] = b[maxx][i] = bb;
            gr[minx][i] = gr[maxx][i] = 128;
            bw[minx][i] = bw[maxx][i] = 128;
        }
    }

    void createPoint(int x, int y, int w, int h, int rr, int gg, int bb) {
        for (int i = Math.max(0, x - 1); i <= Math.min(w - 1, x + 1); i++) {
            for (int j = Math.max(0, y - 1); j <= Math.min(h - 1, y + 1); j++) {
                r[i][j]  = rr;
                g[i][j] = gg;
                b[i][j] = bb;
                gr[i][j] = 128;
                bw[i][j] = 128;
            }
        }
    }

    void getFace() {
        int w = rawBitmap.getWidth();
        int h = rawBitmap.getHeight();

        gr = faceDetector.getSkin(a, r, g, b, w, h);
        gr = faceDetector.preprocess(gr, w, h);
        ArrayList<int[]> boundFace = faceDetector.getFace(gr, w, h);
        bw = faceDetector.convolute(r, g, b, w, h, 90);
        bw = faceDetector.preprocess(bw, w, h);
        for (int[] bound : boundFace) {
            int minx = bound[0], maxx = bound[1], miny = bound[2], maxy = bound[3];
            ArrayList<int[]> featureBound = faceDetector.getFeature(bw, minx, maxx, miny, maxy, w, h);
            Log.d("faceFeature", Integer.toString(featureBound.size()));
            if (featureBound.size() >= 3) {
                if (featureBound.get(featureBound.size() - 1).length <= 1) {
                    maxy = featureBound.get(featureBound.size() - 1)[0];
                    featureBound.remove(featureBound.size() - 1);
                }
                for (int[] b : featureBound) {
                    createRectangle(b[0], b[1], b[2], b[3], 0, 255, 0);
                }
                createRectangle(minx, maxx, miny, maxy, 0, 255, 0);

                int num_points = 6;

                for (int i = 0; i < 4; i++) {
                    if (featureBound.size() > i) {
                        Log.d("controlpoint", "New Feature");

                        ArrayList<int[]> points;

                        if (i < 2) {
                            points = faceDetector.getEyesAndMouthControlPoints(bw,
                                    featureBound.get(i), num_points, false);
                        } else {
                            faceDetector.fill(bw, featureBound.get(i));
                            points = faceDetector.getEyesAndMouthControlPoints(bw,
                                    featureBound.get(i), num_points, true);
                        }

                        for (int[] point : points) {
                            Log.d("controlpoint", Integer.toString(point[0]) + ' ' + Integer.toString(point[1]));
                            createPoint(point[0], point[1], w, h, 0, 255, 0);
                        }
                    }
                }
                if (featureBound.size() > 4) {
                    ArrayList<int[]> points = faceDetector.getNoseControlPoints(bw, featureBound.get(4));
                    for (int[] point: points) {
                        createPoint(point[0], point[1], w, h, 0, 255, 0);
                    }
                }
                if (featureBound.size() > 5) {
                    faceDetector.fill(bw, featureBound.get(5));
                    ArrayList<int[]> points = faceDetector.getEyesAndMouthControlPoints(bw,
                            featureBound.get(5),num_points, true);
                    for (int[] point : points) {
                        createPoint(point[0], point[1], w, h, 0, 255, 0);
                    }
                }
            }
        }

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int colour = Color.rgb(r[i][j], g[i][j], b[i][j]);
                //int colour = Color.rgb(gr[i][j], gr[i][j], gr[i][j]);
//                int colour = Color.rgb(bw[i][j], bw[i][j], bw[i][j]);
                curBitmap.setPixel(i, j, colour);
            }
        }
    }
}

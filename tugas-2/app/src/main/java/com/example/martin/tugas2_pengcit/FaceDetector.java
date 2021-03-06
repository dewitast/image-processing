package com.example.martin.tugas2_pengcit;

import android.graphics.Color;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class FaceDetector {

    private ConvolutionProcessor processor = new ConvolutionProcessor();

    double[][] getHSV(int[][] r, int[][] g, int[][] b, int w, int h, int index) {
        double[][] result = new double[w][h];
        float[] hsv = new float[3];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                Color.RGBToHSV(r[i][j], g[i][j], b[i][j], hsv);
                result[i][j] = hsv[index];
            }
        }

        return result;
    }

    public double[][] getY(int[][] r, int[][]g, int[][] b, int w, int h) {
        double[][] y = new double[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                double rr = (double) r[i][j] / 255, bb = (double) b[i][j] / 255, gg = (double) g[i][j] / 255;
                y[i][j] = 16 + 65.481*rr + 128.553*gg + 24.966*bb;
            }
        }

        return y;
    }

    public double[][] getCr(int[][] r, int[][] g, int[][] b, int w, int h) {
        double[][] result = new double[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                double rr = (double) r[i][j] / 255, bb = (double) b[i][j] / 255, gg = (double) g[i][j] / 255;
                result[i][j] = 128 - 37.7745*rr - 74.1592*gg + 111.9337*bb;
            }
        }

        return result;
    }

    public double[][] getCb(int[][] r, int[][] g, int[][] b, int w, int h) {
        double[][] result = new double[w][h];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                double rr = (double) r[i][j] / 255, bb = (double) b[i][j] / 255, gg = (double) g[i][j] / 255;
                result[i][j] = 128 + 111.9581*rr - 93.7509*gg - 18.2072*bb;
            }
        }

        return result;
    }

    int[][] getSkin(int[][] a, int[][] r, int[][] g, int[][] b, int w, int h) {
        double[][] hue = getHSV(r, g, b, w, h, 0);
        double[][] sat = getHSV(r, g, b, w, h, 1);
        double[][] yM = getY(r, g, b, w, h);
        double[][] crM = getCr(r, g, b, w, h);
        double[][] cbM = getCb(r, g, b, w, h);
        
        int[][] gr = new int[w][h];
        double y,cb,cr;

        boolean[] c = new boolean[10];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                gr[i][j] = 0;

                c[0] = (0<=hue[i][j] && hue[i][j]<=50 && 0.23<=sat[i][j] && sat[i][j]<=0.68);
                c[1] = (r[i][j]>95 && g[i][j]>40 && b[i][j]>20 && r[i][j]>g[i][j]);
                c[2] = (r[i][j]>b[i][j] && Math.abs(r[i][j]-g[i][j])>15 && a[i][j]>15);

                if (c[0] && c[1] && c[2]) {
                    gr[i][j] = 255;
                }

                cb = cbM[i][j]; cr = crM[i][j]; y = yM[i][j];
                c[0] = (cr>135 && cb>85 && y>80 && cr<=1.5862*cb+20 && cr<=-1.15*cb+301.75);
                c[3] = (cr>=0.3448*cb+76.2069 && cr>=-4.5652*cb+234.5652 && cr<=-2.2857*cb+432.85);
                if (c[0] && c[1] && c[2] && c[3]) {
                    gr[i][j] = 255;
                }
            }
        }
        return gr;
    }

    int[][] preprocess(int[][] gr, int w, int h) {
        return processor.smoothing(gr, w, h);
    }

    int[][] convolute(int[][] r, int[][] g, int[][] b, int w, int h, int th) {
        r = processor.sobel(r, w, h);
        g = processor.sobel(g, w, h);
        b = processor.sobel(b, w, h);

        ImageProcessor imageProcessor = new ImageProcessor();
        int[][] bw = imageProcessor.convertToBW(r, g, b, w, h, th);
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (bw[i][j] > 0) {
                    bw[i][j] = 255;
                } else {
                    bw[i][j] = 0;
                }
            }
        }
        return bw;
    }

    private boolean[][] visited;
    private int maxx, maxy, minx, miny;

    boolean isValid(int x, int y, int w, int h) {
        return (x>=0 && x<w && y>=0 && y<h);
    }

    int dfs(int[][] gr, int x, int y, int w, int h, int color) {
        visited[x][y] = true;
        int sum = 1;
        maxx = Math.max(maxx, x);
        minx = Math.min(minx, x);
        maxy = Math.max(maxy, y);
        miny = Math.min(miny, y);

        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};

        for (int k = 0; k < dx.length; k++) {
            if (isValid(x+dx[k], y+dy[k], w, h) && !visited[x+dx[k]][y+dy[k]] && gr[x+dx[k]][y+dy[k]] == color) {
                sum += dfs(gr, x+dx[k], y+dy[k], w, h, color);
            }
        }

        return sum;
    }

    int dfsReset(int[][] gr, int x, int y, int w, int h, int color) {
        visited[x][y] = false;
        int sum = 1;
        maxx = Math.max(maxx, x);
        minx = Math.min(minx, x);
        maxy = Math.max(maxy, y);
        miny = Math.min(miny, y);

        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};

        for (int k = 0; k < dx.length; k++) {
            if (isValid(x+dx[k], y+dy[k], w, h) && visited[x+dx[k]][y+dy[k]] && gr[x+dx[k]][y+dy[k]] == color) {
                sum += dfsReset(gr, x+dx[k], y+dy[k], w, h, color);
            }
        }

        return sum;
    }

    ArrayList<int[]> getFace(int[][] gr, int w, int h) {
        visited = new boolean[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                visited[i][j] = false;
            }
        }

        ArrayList<int[]> candidate = new ArrayList<>();
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                if (gr[i][j]>0 && !visited[i][j]) { // putih
                    maxx = i; minx = i;
                    maxy = j; miny = j;
                    int sum = dfs(gr, i, j, w, h, 255);
                    if (sum >= 100) {
                        int[] result = {minx, maxx, miny, maxy};
                        candidate.add(result);
                    }
                }
            }
        }
        return candidate;
    }

    void resetOutside(int[][] gr, int w, int h) {
        ArrayList<int[]> q = new ArrayList<>();
        q.add(new int[]{0, 0});
        visited[0][0] = true;
        int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
        int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};
        while (q.size() > 0) {
            int[] a = q.get(0);
            int x = a[0], y = a[1];
            q.remove(0);

            for (int k = 0; k < dx.length; k++) {
                if (isValid(x+dx[k], y+dy[k], w, h) && !visited[x+dx[k]][y+dy[k]] && gr[x+dx[k]][y+dy[k]] == 0) {
                    visited[x+dx[k]][y+dy[k]] = true;
                    q.add(new int[]{x+dx[k], y+dy[k]});
                }
            }
        }
    }

    ArrayList<int[]> getFeature(int[][] gr, int cminx, int cmaxx, int cminy, int cmaxy, int w, int h) {
        for (int i = cminx; i <= cmaxx; i++) {
            for (int j = cminy; j <= cmaxy; j++) {
                visited[i][j] = false;
            }
        }

        int tinggi = cmaxy-cminy+1;
        int deltaMulut = 0;

        // traverse top and bottom
        for (int i = cminx+1; i <= cmaxx-1; i++) {
            if (gr[i][cminy+1] == 0 && !visited[i][cminy+1]) {
                dfs(gr, i, cminy+1, w, h, 0);
            }

            if (gr[i][cmaxy-1] == 0 && !visited[i][cmaxy-1]) {
                dfs(gr, i, cmaxy-1, w, h, 0);
            }
        }

        // traverse left and right
        for (int j = cminy+1; j <= cmaxy-1; j++) {
            if (gr[cminx+1][j] == 0 && !visited[cminx+1][j]) {
                dfs(gr, cminx+1, j, w, h, 0);
            }

            if (gr[cmaxx-1][j] == 0 && !visited[cmaxx-1][j]) {
                dfs(gr, cmaxx-1, j, w, h, 0);
            }
        }

        ArrayList<int[]> result = new ArrayList<>();

        int cmidx = (cminx + cmaxx) / 2;
        int eyemaxy = 0;
        int featureCount = 1;
        for (int j = cminy + 1; j < cmaxy; j++) {
            if (featureCount <= 2) {
                int xleft = -1, yleft = -1, xright = -1, yright = -1;
                int lminx = -1, lmaxx = -1, lminy = -1, lmaxy = -1;
                int rminx = -1, rmaxx = -1, rminy = -1, rmaxy = -1;
                boolean found = false;
                int thresholdL = (featureCount == 1)? 50: 80;

                // get right
                int size_right = 0;
                for (int i = cmidx; i <= cmaxx; i++) {
                    if (gr[i][j] == 0 && !visited[i][j]) {
                        maxx = i;
                        minx = i;
                        miny = j;
                        maxy = j;
                        dfs(gr, i, j, w, h, 0);
                        rminx = minx;
                        rmaxx = maxx;
                        rminy = miny;
                        rmaxy = maxy;
                        xright = i;
                        yright = j;
                        size_right = (rmaxx-rminx+1)*(rmaxy-rminy+1);
                        if (size_right > thresholdL) {
                            break;
                        }
                    }
                }

                int size_left = 0;
                // get left
                for (int k = Math.max(cminy + 1, j - 5); k < Math.min(cmaxy - 1, j + 5); k++) {
                    for (int i = cmidx - 1; i >= cminx; i--) {
                        if (gr[i][k] == 0 && !visited[i][k]) {
                            maxx = i;
                            minx = i;
                            miny = k;
                            maxy = k;
                            dfs(gr, i, k, w, h, 0);
                            lminx = minx;
                            lmaxx = maxx;
                            lminy = miny;
                            lmaxy = maxy;
                            xleft = i;
                            yleft = k;
                            size_left = (lmaxx-lminx+1)*(lmaxy-lminy+1);
                            if (size_left > thresholdL) {
                                break;
                            }
                        }
                    }
                    if (size_left > thresholdL) {
                        break;
                    }
                }

                size_left = (lmaxx-lminx+1)*(lmaxy-lminy+1);
                size_right = (rmaxx-rminx+1)*(rmaxy-rminy+1);

                boolean clear = false;
                if (featureCount == 1) {
                    if (lmaxx >= 0 && lmaxy - lminy > (lmaxx - lminx) / 2 && size_left > thresholdL) {
                        int lmidy = lminy + (lmaxy - lminy) / 3;
                        for (int xx = lminx; xx <= lmaxx; xx++) {
                            gr[xx][lmidy] = 255;
                            if (gr[xx][lmidy+1] == 0 && visited[xx][lmidy+1]) {
                                dfsReset(gr, xx, lmidy+1, w, h, 0);
                            }
                        }
                        clear = true;
                    }
                    if (rmaxx >= 0 && rmaxy - rminy > (rmaxx - rminx) / 2 && size_right > thresholdL) {
                        int rmidy = rminy + (rmaxy - rminy) / 3;
                        for (int xx = rminx; xx <= rmaxx; xx++) {
                            gr[xx][rmidy] = 255;
                            if (gr[xx][rmidy+1] == 0 && visited[xx][rmidy+1]) {
                                dfsReset(gr, xx, rmidy+1, w, h, 0);
                            }
                        }
                        clear = true;
                    }
                }

                if (!clear && size_left > thresholdL && size_right > thresholdL  && lmaxx < rminx && size_left * 4 > size_right && size_right * 4 > size_left) {
                    found = true;
                } else {
                    if (xleft != -1) {
                        dfsReset(gr, xleft, yleft, w, h, 0);
                    }
                    if (xright != -1) {
                        dfsReset(gr, xright, yright, w, h, 0);
                    }

                }
                if (found) {
                    if (featureCount == 2) {
                        deltaMulut = ((lminy + rminy) / 2 - cminy) / 2;
                    }
                    result.add(new int[]{lminx, lmaxx, lminy, lmaxy});
                    result.add(new int[]{rminx, rmaxx, rminy, rmaxy});
                    featureCount += 1;
                    eyemaxy = Math.max(lmaxy, rmaxy);
                    j = eyemaxy + 5;
                }
            }

            else if (featureCount <= 4) {
                boolean found = false;
                int hminx = w+1, hminy = h+1, hmaxx = -1, hmaxy = -1;

                // 3 = hidung
                int xmkiri = (featureCount == 3)? result.get(2)[1]:result.get(2)[0];
                int xmnkanan = (featureCount == 3)?result.get(3)[0]:result.get(3)[1];

                int thresholdL = (featureCount == 3)? 100: 200;
                int thresholdU = (featureCount == 3)? 120701: 120701;

                ArrayList<int[]> points = new ArrayList<>();
                for (int i = xmkiri; i <= xmnkanan; i++) {
                    if (gr[i][j] == 0 && !visited[i][j]) {
                        maxx = i; minx = i; maxy = j; miny = j;
                        dfs(gr, i, j, w, h,0);

                        points.add(new int[]{i, j});
                        hminx = Math.min(hminx, minx);
                        hmaxx = Math.max(hmaxx, maxx);
                        hminy = Math.min(hminy, miny);
                        hmaxy = Math.max(hmaxy, maxy);
                    }
                }

                int luas = (hmaxx - hminx + 1) * (hmaxy - hminy + 1);
                int hmidx = (hmaxx + hminx) / 2;

                boolean clear = false;
                if (featureCount == 3) {
                    if (hmaxx >= 0 && hmaxy - hminy > (hmaxx - hminx) * 9 / 10 && luas > thresholdL) {
                        int hmidy = (hmaxy + hminy) / 2;
                        for (int xx = hminx; xx <= hmaxx; xx++) {
                            gr[xx][hmidy] = 255;
                            if (gr[xx][hmidy+1] == 0 && visited[xx][hmidy+1]) {
                                dfsReset(gr, xx, hmidy+1, w, h, 0);
                            }
                        }
                        clear = true;
                    }
                }

                if (!clear && luas > thresholdL && luas < thresholdU && hminx <= cmidx && hmaxx > cmidx && Math.abs(hmidx-cmidx) < 20) {
                    found = true;
                } else {
                    for (int[] p : points) {
                        dfsReset(gr, p[0], p[1], w, h, 0);
                    }
                }

                if (found) {
                    for (int y = hminy; y <= hmaxy; y++) {
                        for (int i = xmkiri; i <= xmnkanan; i++) {
                            if (gr[i][y] == 0 && !visited[i][y]) {
                                maxx = i; minx = i; maxy = y; miny = y;
                                int size = dfs(gr, i, y, w, h,0);

                                if (size > 10) {
                                    points.add(new int[]{i, y});
                                    hminx = Math.min(hminx, minx);
                                    hmaxx = Math.max(hmaxx, maxx);
                                    hminy = Math.min(hminy, miny);
                                    hmaxy = Math.max(hmaxy, maxy);
                                    
                                }
                            }
                        }
                    }
                    if (featureCount != 4 || hminy >= result.get(result.size()-1)[3]) {
                        if (featureCount == 4) {
                            hmaxy += 5;
                            deltaMulut += hmaxy - 5;
                        }
                        result.add(new int[]{hminx, hmaxx, hminy, hmaxy});
                        if (featureCount == 4) {
                            result.add(new int[]{deltaMulut});
                        }
                        featureCount += 1;
                        j = hmaxy + 1;
                    }
                }
            }
        }


        if (result.size() >= 4) {
            for (int i = 0; i < 2; i++) {
                int[] alis = result.get(i);
                int[] mata = result.get(i + 2);

                int maxy_alis = alis[3];
                int miny_mata = mata[2];

                int result_maxy_alis = maxy_alis;
                int result_miny_mata = miny_mata;

                if (maxy_alis > miny_mata) {
                    int midy = (maxy_alis + miny_mata) / 2;
                    result_maxy_alis = midy;
                    result_miny_mata = midy + 1;
                }
                alis[3] = result_maxy_alis;
                mata[2] = result_miny_mata;
            }
        }
        return result;
    }

    void fill(int[][] gr, int[] bound) {
        int minx = bound[0] + 1;
        int maxx = bound[1] - 1;
        int miny = bound[2] + 1;
        int maxy = bound[3] - 1;

        int firstx, lastx;
        for (int j = miny + 1; j < maxy; j++) {
            firstx = lastx = -1;
            for (int i = minx + 1; i < maxx; i++) {
                if (gr[i][j] == 0) {
                    lastx = i;
                    if (firstx < 0) {
                        firstx = i;
                    }
                }
            }
            if (firstx > 0) {
                for (int i = firstx; i <= lastx; i++) {
                    gr[i][j] = 0;
                }
            }
        }
    }

    ArrayList<int[]> getNoseControlPoints(int[][] gr, int[] bound) {
        int minx = bound[0] + 1;
        int maxx = bound[1] - 1;
        int miny = bound[2] + 1;
        int maxy = bound[3] - 1;

        int[] left = new int[2];
        int[] mid = new int[2];
        int[] right = new int[2];

        int range_x = maxx - minx;
        int x_first = minx + range_x / 4;
        int x_second = maxx - range_x / 4;
        int x_mid= (x_first + x_second) / 2;

        int y_first = maxy;
        int y_second = maxy;

        for (int j = maxy; j >= miny; j--) {
            if (gr[x_first][j] == 0) {
                y_first = j;
                break;
            }
        }

        for (int j = maxy; j >= miny; j--) {
            if (gr[x_second][j] == 0) {
                y_second = j;
                break;
            }
        }

        left[0] = x_first;
        left[1] = y_first;

        right[0] = x_second;
        right[1] = y_second;

        mid[0] = x_mid;
        mid[1] = Math.min(maxy, 2 + (y_first + y_second) / 2);

        ArrayList<int[]> result = new ArrayList<>();

        result.add(left);
        result.add(right);
        result.add(mid);

        return result;
    }

    ArrayList<int[]> getEyesAndMouthControlPoints(int[][] gr, int[] bound, int k, boolean from_bottom) {
        int minx = bound[0] + 1;
        int maxx = bound[1] - 1;
        int miny = bound[2] + 1;
        int maxy = bound[3] - 1;
        int midx = (minx + maxx) / 2;

        int middle_split = 1 + (k - 2) / 2;
        ArrayList<int[]> points = new ArrayList<>();

        // get left
        int[] left = new int[2];
        for (int j = miny; j <= maxy; j++) {
            if (gr[minx][j] == 0) {
                left[0] = minx;
                left[1] = j;
                break;
            }
        }

        // get right
        int[] right = new int[2];

        for (int j = miny; j <= maxy; j++) {
            if (gr[maxx][j] == 0) {
                right[0] = maxx;
                right[1] = j;
                break;
            }
        }

        // add left
        points.add(left);

        int delta_x = (maxx - minx) / middle_split;
        int current_x = minx;
        ArrayList<int[]> tops = new ArrayList<>();
        ArrayList<int[]> bottoms = new ArrayList<>();

        for (int split = 1; split < middle_split; split++) {
            current_x = current_x + delta_x;

            // get top mid
            int[] top = new int[2];
            for (int j = miny; j <= maxy; j++) {
                if (gr[current_x][j] == 0) {
                    top[0] = current_x;
                    top[1] = j;
                    break;
                }
            }

            // get bottom mid
            int[] bottom = new int[2];
            if (from_bottom) {
                for (int j = maxy; j >= miny; j--) {
                    if (gr[current_x][j] == 0) {
                        bottom[0] = current_x;
                        bottom[1] = j;
                        break;
                    }
                }
            } else {
                for (int j = top[1]; j <= maxy - 1; j++) {
                    if (gr[current_x][j] == 0 && gr[current_x][j + 1] == 255) {
                        bottom[0] = current_x;
                        bottom[1] = j;
                        break;
                    }
                }
                if (bottom[0] == 0 && bottom[1] == 0) {
                    bottom[0] = current_x;
                    bottom[1] = maxy;
                }
            }

            tops.add(top);
            bottoms.add(bottom);
        }

        // add tops
        points.addAll(tops);

        // add right
        points.add(right);

        Collections.reverse(bottoms);
        points.addAll(bottoms);

        return points;
    }

    double gradient(int[] p1, int[] p2) {
        double g = Math.atan2(p1[1] - p2[1], p1[0] - p2[0]);
        if (g < 0) {
            g += 2 * Math.PI;
        }
        return g;
    }
    
    double compare(ArrayList<int[]> p1, ArrayList<int[]> p2) {
        double delta = 0;

        for (int i = 1; i < p1.size(); i++) {
            double g1 = gradient(p1.get(i), p1.get(i-1));
            double g2 = gradient(p2.get(i), p2.get(i-1));
            if (g1 > g2) {
                double temp = g1;
                g1 = g2;
                g2 = temp;
            }
            delta += Math.min(g2-g1, 2 * Math.PI - g2 + g1) * 180 / Math.PI;
        }

        double g1 = gradient(p1.get(p1.size() - 1), p1.get(0));
        double g2 = gradient(p2.get(p2.size() - 1), p2.get(0));
        if (g1 > g2) {
            double temp = g1;
            g1 = g2;
            g2 = temp;
        }
        delta += Math.min(g2-g1, 2 * Math.PI - g2 + g1) * 180 / Math.PI;
        return delta;
    }
}

package com.qbaor.android_ocr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.qbaor.android_ocr.utils.PermissionUtil;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private ImageCapture imageCapture;
    private PreviewView surfacePreview;
    private Camera camera;
    private Preview preview;
    private Button capture;
    private ImageView img;
    public static String local_data = "";
    public static String temp_folder = "";
    public static  String PATH  = "";
    public static String[] lan ={"eng"};

    TessBaseAPI tessBaseAPI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.img);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                img.setVisibility(View.GONE);
            }
        });
        PATH = getFilesDir().getAbsolutePath().toString();
        local_data =  PATH + "/tessdata/";
        Log.d(TAG, "onCreate: " + local_data);
        saveAssetDataToLocalFile();
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.setDebug(false);
        tessBaseAPI.init(PATH + File.separator, "eng");
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
        PermissionUtil.requestRunTimePermission(this);
        initView();
        initData();
    }

    private String saveAssetDataToLocalFile() {
        InputStream initialStream = null;
        File targetFile = null;

        try {
            for(String temp: lan) {
                initialStream = getAssets().open(temp+".traineddata");
                byte[] buffer = new byte[initialStream.available()];
                initialStream.read(buffer);
                targetFile = new File(local_data+temp+".traineddata");
                if (targetFile.getParentFile().exists()) {
                    Log.d(TAG, "getAssetDataToLocalFile: ??????????????????");
                } else {
                    Log.d(TAG, "getAssetDataToLocalFile: ????????????");
                    targetFile.getParentFile().mkdirs();
                }
                OutputStream outStream = null;
                outStream = new FileOutputStream(targetFile);
                outStream.write(buffer);
                Log.d(TAG, "getAssetDataToLocalFile: lan:"+temp+"|???????????????????????????:" + targetFile.length());

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "getAssetDataToLocalFile: target" + targetFile.length());
        }
        return local_data;

    }

    private void initView() {
        surfacePreview = findViewById(R.id.surfacePreview);
        capture = findViewById(R.id.capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //????????????
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.c1111).copy(Bitmap.Config.ARGB_8888, true);
                startOcr(bitmap);
//                takePhoto();
            }
        });
    }

    private String TAG = MainActivity.class.getSimpleName();

    private void startOcr(Bitmap bitmap) {

        tessBaseAPI.setImage(bitmap);
        Log.d(TAG, "startOcr: ");
        //?????????????????????????????????words
        String result = tessBaseAPI.getUTF8Text();
        toast(result);
        System.out.println( "OCR result: " + result);
        //?????????????????????????????????
        Pixa area = tessBaseAPI.getRegions();
        for (Rect temp : area.getBoxRects()) {
            System.out.println("box: " + temp.toShortString());
        }
        showBox(bitmap,area,Color.RED);
        Pixa a = tessBaseAPI.getWords();
        for(Rect aa :a.getBoxRects())
            System.out.println("word box: "+aa.toShortString());
//        showBox(bitmap,a,Color.GREEN);/**/
        img.setVisibility(View.VISIBLE);
        img.setImageBitmap(bitmap);
    }
    public void toast(String s){
        Toast.makeText(this,s,Toast.LENGTH_LONG).show();
    }

    public void showBox(Bitmap src,Pixa a,int color){
        //??????????????????
        for(Rect aa: a.getBoxRects()){
            drawRect(src,color,aa.left,aa.top,aa.width(),aa.height());
        }
    }

    public Bitmap drawRect(Bitmap bitmap,int color,int x,int y,int width,int height){
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        canvas.drawRect(x,y,x+width,y+height,paint);
        return bitmap;
    }
    private final void initData() {
        String var1;
        boolean var2;
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != 0) {
            var1 = "===>????????????";
            var2 = false;
            System.out.println(var1);
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.CAMERA")) {
                var1 = "===>shouldShowRequestPermissionRationale";
                var2 = false;
                System.out.println(var1);
            } else {
                var1 = "===>not shouldShowRequestPermissionRationale";
                var2 = false;
                System.out.println(var1);
            }

            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA"}, 2000);
        } else {
            var1 = "===>????????????????????????";
            var2 = false;
            System.out.println(var1);
            this.startCamera();
        }
    }

    private String copyAssetGetFilePath(String fileName) {
        Log.d(TAG, "copyAssetGetFilePath: 1");
        try {
            File cacheDir = getCacheDir();
            if (!cacheDir.exists()) {
                Log.d(TAG, "copyAssetGetFilePath: 2");

                cacheDir.mkdirs();
            }
            File outFile = new File(cacheDir, fileName);
            if (!outFile.exists()) {
                Log.d(TAG, "copyAssetGetFilePath: 3");

                boolean res = outFile.createNewFile();
                if (!res) {
                    Log.d(TAG, "copyAssetGetFilePath: 4");

                    return null;
                }
            } else {
                if (outFile.length() > 10) {//????????????????????????
                    Log.d(TAG, "copyAssetGetFilePath: 5");
                    return outFile.getPath();
                }
            }
            InputStream is = getAssets().open(fileName);
            FileOutputStream fos = new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int byteCount;
            while ((byteCount = is.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            Log.d(TAG, "copyAssetGetFilePath: 6");
            fos.flush();
            is.close();
            fos.close();
            return outFile.getPath();
        } catch (IOException e) {
            Log.d(TAG, "copyAssetGetFilePath: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private final void startCamera() {
        System.out.println("????????????");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        ProcessCameraProvider cameraProvider = null;
        try {
            cameraProvider = cameraProviderFuture.get();
            // ????????????
            preview = new Preview.Builder().build(); //  UseCase??????Preview
            // ????????????
            imageCapture = new ImageCapture.Builder().build();// UseCase??????ImageCapture

            // ??????????????????????????????/?????????
            CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

            try {
                // ?????????????????????????????????
                cameraProvider.unbindAll();
                // ????????????????????????????????????UseCase????????????
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
                // ?????????????????????view
                if (preview != null && surfacePreview != null && camera != null) {
                    preview.setSurfaceProvider(surfacePreview.createSurfaceProvider(camera.getCameraInfo()));
                }

            } catch (Exception e) {
                System.out.println("Use case????????????");
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private final void takePhoto() {
        // ??????????????????
        String imagePath = getFilesDir().getAbsolutePath() + "/pic_" + System.currentTimeMillis() + ".jpg";// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????+???/pic/test.jpg???
        File file = new File(imagePath);
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        // ????????????
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull @NotNull ImageCapture.OutputFileResults outputFileResults) {
                        Bitmap resource = BitmapFactory.decodeFile(imagePath).copy(Bitmap.Config.ARGB_8888, true);
                        //????????????ocr??????
                        startOcr(resource);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        System.out.println(exception.getImageCaptureError());
                    }
                });
    }


}
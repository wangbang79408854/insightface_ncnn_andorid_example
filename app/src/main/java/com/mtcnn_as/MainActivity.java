package com.mtcnn_as;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.core.app.ActivityCompat;

//import com.google.android.gms.appindexing.AppIndex;
//import com.google.android.gms.common.api.GoogleApiClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {
    private static final int SELECT_IMAGE = 1;
    private static final int SELECT_IMAGE2 = 2;

    private TextView infoResult;
    private ImageView imageView;
    private ImageView imageViewCompare;
    private Bitmap yourSelectedImage = null;
    private Bitmap yourSelectedCompareImage = null;

    EditText etMinFaceSize,etTestTimeCount,etThreadsNumber;
    private int minFaceSize = 40;
    private int testTimeCount = 10;
    private int threadsNumber = 4;

    private boolean maxFaceSetting = false;

    private MTCNN mtcnn = new MTCNN();


    int faceInfo[] = null;
    int faceCompareInfo[] = null;


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    private GoogleApiClient client;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };


    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);
        //模型初始化
        File sdDir = Environment.getExternalStorageDirectory();//获取跟目录

       String path =  sdDir.getAbsoluteFile().getAbsolutePath();



        String sdPath = sdDir.toString() + "/mtcnn/";
        mtcnn.FaceDetectionModelInit(sdPath);

        infoResult = (TextView) findViewById(R.id.infoResult);
        imageView = (ImageView) findViewById(R.id.imageView);
        imageViewCompare = (ImageView) findViewById(R.id.imageViewCompare);

        etMinFaceSize = findViewById(R.id.etMinFaceSize);
        etTestTimeCount =  findViewById(R.id.etTestTimeCount);
        etThreadsNumber =  findViewById(R.id.etThreadsNumber);

        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        Button buttonSSD = (Button) findViewById(R.id.buttonSSD);
        buttonSSD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE2);
            }
        });

        Button buttonDetect = (Button) findViewById(R.id.buttonDetect);
        buttonDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (yourSelectedImage == null)
                    return;

                minFaceSize = Integer.valueOf(TextUtils.isEmpty(etMinFaceSize.getText().toString()) ? "40" : etMinFaceSize.getText().toString());
                testTimeCount = Integer.valueOf(TextUtils.isEmpty(etTestTimeCount.getText().toString()) ? "10" : etTestTimeCount.getText().toString());
                threadsNumber = Integer.valueOf(TextUtils.isEmpty(etThreadsNumber.getText().toString()) ? "4" : etThreadsNumber.getText().toString());

                if (threadsNumber != 1&&threadsNumber != 2&&threadsNumber != 4&&threadsNumber != 8){
                    Log.i(TAG, "线程数："+threadsNumber);
                    infoResult.setText("线程数必须是（1，2，4，8）之一");
                    return;
                }

                Log.i(TAG, "最小人脸："+minFaceSize);
                mtcnn.SetMinFaceSize(minFaceSize);
                mtcnn.SetTimeCount(testTimeCount);
                mtcnn.SetThreadsNumber(threadsNumber);

                //检测流程
                int width = yourSelectedImage.getWidth();
                int height = yourSelectedImage.getHeight();
                byte[] imageDate = getPixelsRGBA(yourSelectedImage);

                long timeDetectFace = System.currentTimeMillis();

                if(!maxFaceSetting) {
                    faceInfo = mtcnn.FaceDetect(imageDate, width, height, 4);
                    Log.i(TAG, "检测所有人脸");
                }
                else{
                    faceInfo = mtcnn.MaxFaceDetect(imageDate, width, height, 4);
                    Log.i(TAG, "检测最大人脸");
                }
                timeDetectFace = System.currentTimeMillis() - timeDetectFace;
                Log.i(TAG, "人脸平均检测时间："+timeDetectFace/testTimeCount);

                if(faceInfo.length>1){
                    int faceNum = faceInfo[0];
                    infoResult.setText("图宽："+width+"高："+height+"人脸平均检测时间："+timeDetectFace/testTimeCount+" 数目：" + faceNum);
                    Log.i(TAG, "图宽："+width+"高："+height+" 人脸数目：" + faceNum );

                    Bitmap drawBitmap = yourSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                    for(int i=0;i<faceNum;i++) {
                        int left, top, right, bottom;
                        Canvas canvas = new Canvas(drawBitmap);
                        Paint paint = new Paint();
                        left = faceInfo[1+14*i];
                        top = faceInfo[2+14*i];
                        right = faceInfo[3+14*i];
                        bottom = faceInfo[4+14*i];

                        paint.setColor(Color.RED);
                        paint.setStyle(Paint.Style.STROKE);//不填充
                        paint.setStrokeWidth(5);  //线的宽度
                        canvas.drawRect(left, top, right, bottom, paint);
                        //画特征点
                        canvas.drawPoints(new float[]{
                                faceInfo[5+14*i],faceInfo[10+14*i],
                                faceInfo[6+14*i],faceInfo[11+14*i],
                                faceInfo[7+14*i],faceInfo[12+14*i],
                                faceInfo[8+14*i],faceInfo[13+14*i],
                                faceInfo[9+14*i],faceInfo[14+14*i]}, paint);//画多个点
                    }
                    imageView.setImageBitmap(drawBitmap);
                }else{
                    infoResult.setText("未检测到人脸");
                }

            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
//        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();

            try {
                if (requestCode == SELECT_IMAGE) {
                    Bitmap bitmap = decodeUri(selectedImage);
                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    // resize to 227x227
                    //yourSelectedImage = Bitmap.createScaledBitmap(rgba, 227, 227, false);
                    yourSelectedImage = rgba;
                    imageView.setImageBitmap(yourSelectedImage);
                }else if (requestCode == SELECT_IMAGE2){
                    Bitmap bitmap = decodeUri(selectedImage);
                    Bitmap rgba = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    // resize to 227x227
                    //yourSelectedImage = Bitmap.createScaledBitmap(rgba, 227, 227, false);
                    yourSelectedCompareImage = rgba;
                    imageViewCompare.setImageBitmap(yourSelectedCompareImage);
                    DetectCompare();
                }
            } catch (FileNotFoundException e) {
                Log.e("MainActivity", "FileNotFoundException");
                return;
            }
        }
    }

    private void DetectCompare() {

        //检测流程
//        int width = yourSelectedCompareImage.getWidth();
//        int height = yourSelectedCompareImage.getHeight();
//
//        byte[] origin = getPixelsRGBA(yourSelectedImage);

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] imageDate = getPixelsRGBA(yourSelectedCompareImage);
                File sdDir = Environment.getExternalStorageDirectory();//获取跟目录
                String sdPath = sdDir.toString() + "/mtcnn/"+"4.jpg";
                Bitmap processbp = yourSelectedCompareImage.copy(Bitmap.Config.ARGB_8888, true);
                Bitmap compare = yourSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
                boolean b = mtcnn.FaceCompare(processbp, compare);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoResult.setText(b?"相似度很高":"相似度不高");
                    }
                });
            }
        }).start();
//        mtcnn.FaceCompare(sdPath,origin,imageDate,yourSelectedImage.getWidth(),yourSelectedImage.getHeight(),width,height);
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 400;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

    //提取像素点
    private byte[] getPixelsRGBA(Bitmap image) {
        // calculate how many bytes our image consists of
        int bytes = image.getByteCount();
        ByteBuffer buffer = ByteBuffer.allocate(bytes); // Create a new buffer
        image.copyPixelsToBuffer(buffer); // Move the byte data to the buffer
        byte[] temp = buffer.array(); // Get the underlying array containing the

        return temp;
    }

    private void copyBigDataToSD(String strOutFileName) throws IOException {
        Log.i(TAG, "start copy file " + strOutFileName);
        File sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        File file = new File(sdDir.toString()+"/mtcnn/");
        if (!file.exists()) {
            file.mkdir();
        }

        String tmpFile = sdDir.toString()+"/mtcnn/" + strOutFileName;
        File f = new File(tmpFile);
        if (f.exists()) {
            Log.i(TAG, "file exists " + strOutFileName);
            return;
        }
        InputStream myInput;
        java.io.OutputStream myOutput = new FileOutputStream(sdDir.toString()+"/mtcnn/"+ strOutFileName);
        myInput = this.getAssets().open(strOutFileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
        Log.i(TAG, "end copy file " + strOutFileName);

    }
}

package com.example.qcameraq;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.CameraOptions;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.PictureFormat;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private Handler handler = new Handler();

    private GalleyAdapter galleyAdapter;

    private String pic_Format = "Pic.jpg";
    private String video_Format = "Video.mp4";

    private File sd = Environment.getExternalStorageDirectory().getAbsoluteFile();
    private File videoDir = new File(sd + File.separator,
            System.currentTimeMillis() + video_Format);
    private File picDir = new File(sd + File.separator,
            System.currentTimeMillis() + pic_Format);

    private Animation anim_Rotate_Front, anim_Rotate_Back;

    private static final int REQUEST_CODE_PERMISSIONS = 786;
    private final static String[] REQUIRED_PERMISSIONS = new String[]
            {
                    "android.permission.Camera",
                    "android.permission.RECORD_AUDIO",
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.READ_EXTERNAL_STORAGE"
            };

    private long timeInMilliseconds = 0L,
            startTime = SystemClock.uptimeMillis(),
            updatedTime = 0L,
            timeSwapBuff = 0L;

    private ImageView btn_cap, btn_rotate, btn_flash;
    private TextView tv_timer, tv_Grid;

    private CameraView camera;
    private TextView tv_Holdtap;

    private RecyclerView rv_gallery;
    private Bitmap bitmap;
//    private SharedPre sharedPre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        allPermissionGranted(REQUEST_CODE_PERMISSIONS, REQUIRED_PERMISSIONS);

//        sharedPre = new SharedPre(this);

//        checkCameraOption();

        galleyAdapter = new GalleyAdapter();
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        camera.setLifecycleOwner(this);
        camera.setPictureFormat(PictureFormat.JPEG);
        camera.setVideoMaxDuration(6000);

        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraError(@NonNull CameraException exception) {
                super.onCameraError(exception);
                Log.e(TAG,"Got CameraException #" + exception.getReason());
            }

            @Override
            public void onExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
                super.onExposureCorrectionChanged(newValue, bounds, fingers);
                message("Exposure correction:$newValue", false);
            }

            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                super.onPictureTaken(result);
                Log.e(TAG, "onPictureTaken");
                if(camera.isTakingVideo()){
                    message("Captured while taking video. Size=" + result.getSize(), false);
                    return;
                }
                result.toFile(picDir, File::deleteOnExit);
            }

            @Override
            public void onVideoTaken(@NonNull VideoResult result) {
                super.onVideoTaken(result);
                Log.e(TAG, "onVideoTaken");
                if(camera.isTakingPicture()){
                    return;
                }
                return;
            }

            @Override
            public void onOrientationChanged(int orientation) {
                super.onOrientationChanged(orientation);
            }

            @Override
            public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
                super.onZoomChanged(newValue, bounds, fingers);
                message("Zoom:" + newValue, false);
            }

            @Override
            public void onVideoRecordingStart() {
                super.onVideoRecordingStart();
                Log.e(TAG, "onVideoRecordingStart");
            }

            @Override
            public void onVideoRecordingEnd() {
                super.onVideoRecordingEnd();
                Log.e(TAG, "onVideoRecordingEnd");
                message("Video taken. Processing...", false);
            }
        });
    }

    private void message(String content, boolean important){
        if(important){
            Toast.makeText(getApplicationContext(), content, Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(getApplicationContext(), content, Toast.LENGTH_SHORT).show();
        }
    }

    private void initView(){
        camera = findViewById(R.id.main_camera);

        btn_cap = findViewById(R.id.btn_main_capture);
        btn_rotate = findViewById(R.id.btn_main_rotate);
        btn_flash = findViewById(R.id.btn_main_flash);
        tv_Grid = findViewById(R.id.tv_main_grid);

        tv_timer = findViewById(R.id.tv_main_timer);
        tv_Holdtap = findViewById(R.id.tv_main_holdtap);

        rv_gallery = findViewById(R.id.rv_main_gallery);

        anim_Rotate_Front = AnimationUtils.loadAnimation(this, R.anim.rotate_front);
        anim_Rotate_Back = AnimationUtils.loadAnimation(this, R.anim.rotate_back);

        activeCameraCapture();

        controlView();
    }

    private void controlView(){
        btn_cap.setOnClickListener(this);
        activeCameraCapture();
        
        btn_flash.setOnClickListener(this);
        btn_rotate.setOnClickListener(this);
        tv_Grid.setOnClickListener(this);

        rv_gallery.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL,
                false));

        rv_gallery.setAdapter(galleyAdapter);
    }

    private void activeCameraCapture() {
        btn_cap.setAlpha(1.0f);
        btn_cap.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                try {
                    Log.e(TAG, "onLongClick: btn_cap");
                    camera.setMode(Mode.VIDEO);
                    camera.takeVideo(videoDir);
                    handler.postDelayed(updateTimerThread, 0);
                } catch(Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "onLongClick: Video Error");
                }
                tv_Grid.setVisibility(View.INVISIBLE);
                btn_rotate.setVisibility(View.INVISIBLE);
                btn_flash.setVisibility(View.INVISIBLE);
                tv_Holdtap.setVisibility(View.INVISIBLE);
                tv_timer.setVisibility(View.VISIBLE);

                try{
                    handler.postDelayed(scaleUpAnimation, 100);
                } catch (Exception e){
                    Log.e(TAG, "onLongClick: scaleUpAnimation()");
                }

                btn_cap.setOnTouchListener(new View.OnTouchListener(){
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS){
                            Log.e(TAG, "onTouch: ACTION_BUTTON_PRESS");
                            capturePicture();
                        }
                        if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                            Log.e(TAG, "onTouch: ACTION_UP");
                            camera.stopVideo();
                            handler.postDelayed(scaleDownAnimation, 100);

                            tv_timer.setVisibility(View.INVISIBLE);

                            tv_Grid.setVisibility(View.VISIBLE);
                            btn_flash.setVisibility(View.VISIBLE);
                            btn_rotate.setVisibility(View.VISIBLE);
                            tv_Holdtap.setVisibility(View.VISIBLE);

                            camera.setMode(Mode.PICTURE);

                            return true;
                        }
                        return true;
                    }
                });
                return true;
            }
        });
        return;
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_main_capture:
                capturePicture();
                break;
            case R.id.btn_main_rotate:
                rotateCamera();
                break;
            case R.id.btn_main_flash:
                flash();
                break;
            case R.id.tv_main_grid:
                changeGrid();
                break;
        }
        return;
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            int hrs = mins / 60;

            secs = secs % 60;
            tv_timer.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
            handler.postDelayed(this, 0);
        }
    };

    private void rotateCamera() {
        if(camera.isTakingVideo() || camera.isTakingPicture()){
            Log.e(TAG, "rotateCamera: Taking Picture or Video");
            return;
        }
        switch (camera.getFacing()) {
            case FRONT:
                camera.setFacing(Facing.BACK);
                btn_rotate.startAnimation(anim_Rotate_Back);
//                sharedPre.setFace(true);
                Log.e(TAG, "rotateCamera: Back");
                break;
            case BACK:
                camera.setFacing(Facing.FRONT);
                btn_rotate.startAnimation(anim_Rotate_Front);
//                sharedPre.setFace(false);
                Log.e(TAG, "rotateCamera: Front");
                break;
        }
        return;
    }

    private void flash() {
        if(camera.isTakingPicture() || camera.isTakingVideo()) { return; }
        switch (camera.getFlash()) {
            case ON:
                camera.setFlash(Flash.AUTO);
                btn_flash.setBackgroundResource(R.drawable.flash_auto_48px);
//                sharedPre.setFlash(2);
                Log.e(TAG, "flash: Auto");
                break;
            case AUTO:
                camera.setFlash(Flash.OFF);
                btn_flash.setBackgroundResource(R.drawable.flash_off_48px);
//                sharedPre.setFlash(0);
                Log.e(TAG, "flash: Off");
                break;
            case OFF:
                camera.setFlash(Flash.ON);
                btn_flash.setBackgroundResource(R.drawable.flash_on_48px);
//                sharedPre.setFlash(1);
                Log.e(TAG, "flash: On");
                break;
        }
        return;
    }


    private void changeGrid() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) { return; }
        switch (camera.getGrid()){
            case OFF:
                camera.setGrid(Grid.DRAW_3X3);
                tv_Grid.setText("3*3");
//                sharedPre.setGrid(1);
                Log.e(TAG, "changeGrid: 3*3");
                break;
            case DRAW_3X3:
                camera.setGrid(Grid.DRAW_4X4);
                tv_Grid.setText("4*4");
//                sharedPre.setGrid(2);
                Log.e(TAG, "changeGrid: 4*4");
                break;
            case DRAW_4X4:
                camera.setGrid(Grid.DRAW_PHI);
                tv_Grid.setText("Grid Phi");
//                sharedPre.setGrid(3);
                Log.e(TAG, "changeGrid: Grid Phi");
                break;
            case DRAW_PHI:
                camera.setGrid(Grid.OFF);
                tv_Grid.setText("Off");
//                sharedPre.setGrid(0);
                Log.e(TAG, "changeGrid: Off");
                break;
        }
        return;
    }

    public void capturePicture(){
        if (camera.getMode() == Mode.VIDEO) {
            message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (camera.isTakingPicture()) { return; }
        message("Capturing picture...", false);
        try {
            camera.takePicture();
        } catch (Exception e){
            Log.e(TAG, "capturePicture: Error Save Picture");
        }
    }

    private void captureVideo() {
        camera.setMode(Mode.VIDEO);
        if (camera.getMode() == Mode.PICTURE) {
            message("Can't record HQ videos while in PICTURE mode.", false);
            return;
        }
        if(camera.isTakingVideo()) { return; }
        message("Recording for 5 seconds...", true);
        camera.takeVideo(videoDir);
    }

    private Runnable scaleUpAnimation = new Runnable() {
        @Override
        public void run() {
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btn_cap, "scaleX", 1f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btn_cap, "scaleY", 1f);
            scaleDownX.setDuration(100);
            scaleDownY.setDuration(100);
            AnimatorSet scaleDown = new AnimatorSet();
            scaleDown.play(scaleDownX).with(scaleDownY);

            scaleDownX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    View p = (View) btn_cap.getParent();
                    p.invalidate();
                }
            });
            scaleDown.start();
        }
    };

    private Runnable scaleDownAnimation = new Runnable() {
        @Override
        public void run() {
            Object target;
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btn_cap, "scaleX", 1f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btn_cap, "scaleY", 1f);
            scaleDownX.setDuration(100);
            scaleDownY.setDuration(100);
            AnimatorSet scaleDown = new AnimatorSet();
            scaleDown.play(scaleDownX).with(scaleDownY);

            scaleDownX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    View p = (View) btn_cap.getParent();
                    p.invalidate();
                }
            });
            scaleDown.start();
        }
    };

    private void allPermissionGranted(int requestCode, String[] REQUIRED_PERMISSIONS){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            } else{
                try {
                    camera.open();
                } catch (Exception e){
                    Log.e(TAG, "allPermissionGranted");
                }
            }
        }
    }

    /*
    private void checkCameraOption(){

        //set flash mode
        switch (sharedPre.getFlash()){
            case 0:
                camera.setFlash(Flash.OFF);
                btn_flash.setBackgroundResource(R.drawable.flash_off_48px);
                break;
            case 1:
                camera.setFlash(Flash.ON);
                btn_flash.setBackgroundResource(R.drawable.flash_on_48px);
                break;
            case 2:
                camera.setFlash(Flash.AUTO);
                btn_flash.setBackgroundResource(R.drawable.flash_auto_48px);
                break;
        }

        //set face mode
        boolean rotated = sharedPre.getRotated();
        if (rotated) {
            camera.setFacing(Facing.BACK);
        } else if (!(rotated)) {
            camera.setFacing(Facing.FRONT);
        }

        //set grid mode
        switch (sharedPre.getGrid()){
            case 0:
                camera.setGrid(Grid.OFF);
                tv_Grid.setText("Off");
            case 1:
                camera.setGrid(Grid.DRAW_3X3);
                tv_Grid.setText("3*3");
            case 2:
                camera.setGrid(Grid.DRAW_4X4);
                tv_Grid.setText("4*4");
            case 3:
                camera.setGrid(Grid.DRAW_PHI);
                tv_Grid.setText("Grid Phi");
        }
    }
     */
}
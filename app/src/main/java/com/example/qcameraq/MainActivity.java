package com.example.qcameraq;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.qcameraq.databinding.ActivityMainBinding;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.otaliastudios.cameraview.CameraException;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraLogger;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.VideoResult;
import com.otaliastudios.cameraview.controls.Facing;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Grid;
import com.otaliastudios.cameraview.controls.Hdr;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.controls.PictureFormat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityMainBinding binding;

    private static final String TAG = "MainActivity";

    private Handler handler = new Handler();

    private String pic_Format = "Pic.jpg";
    private String video_Format = "Video.mp4";

    private File path = Environment.getExternalStorageDirectory().getAbsoluteFile();

    private Animation anim_Rotate_Front, anim_Rotate_Back;

    private String UPLOAD_URL = "1.1.1.1.9999";

    private long timeInMilliseconds = 0L,
            startTime = SystemClock.uptimeMillis(),
            updatedTime = 0L,
            timeSwapBuff = 0L;

    private View rootView;
    private GalleyAdapter adapter;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dexter.withContext(getApplicationContext()).
                withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                if(multiplePermissionsReport.areAllPermissionsGranted()) {
                    binding = ActivityMainBinding.inflate(getLayoutInflater());
                    rootView = binding.getRoot();
                    setContentView(rootView);

                    initView();

                    adapter = new GalleyAdapter();
                    binding.rvGallery.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.HORIZONTAL,
                            false));
                    binding.rvGallery.setAdapter(adapter);

                    //set CameraOption
                    binding.camera.setLifecycleOwner(MainActivity.this);
                    CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
                    binding.camera.setPictureFormat(PictureFormat.JPEG);
                    binding.camera.setVideoMaxDuration(6000);

                    //Events Of Camera
                    binding.camera.addCameraListener(new CameraListener() {
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
                            if(binding.camera.isTakingVideo()){
                                message("Captured while taking video. Size=" + result.getSize(), false);
                                return;
                            }

                            File picDir = new File(path + File.separator, System.currentTimeMillis() + pic_Format);
                            result.toFile(picDir, file -> {});

                            Bitmap bitmap;
                            result.toBitmap(3200, 3200, bmp -> {
                                adapter.addItem(bmp);
                                uploadImage(getImageString(bmp));
                            });
                        }

                        @Override
                        public void onVideoTaken(@NonNull VideoResult result) {
                            super.onVideoTaken(result);
                            Log.e(TAG, "onVideoTaken");
                            if(binding.camera.isTakingPicture()){
                                return;
                            }
                        }

                        @Override
                        public void onOrientationChanged(int orientation) {
                            super.onOrientationChanged(orientation);
                        }

                        @Override
                        public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
                            super.onZoomChanged(newValue, bounds, fingers);
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
                } else{
                    multiplePermissionsReport.getDeniedPermissionResponses();
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

            }
        }).check();
    }

    private void uploadImage(String imageString) {
        binding.pgUpload.setVisibility(View.VISIBLE);
        StringRequest stringRequest = new StringRequest(Request.Method.POST,
                UPLOAD_URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.e(TAG, "onResponse: " + response);
                binding.pgUpload.setVisibility(View.INVISIBLE);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "onErrorResponse: " + error.getMessage().toString());
                binding.pgUpload.setVisibility(View.INVISIBLE);
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> para = new HashMap<String, String>();
                para.put("image", imageString);
                return para;
            }
        };
        stringRequest.setTag(TAG);

        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        requestQueue.add(stringRequest);
        requestQueue.start();
    }

    private void message(String content, boolean important){
        if(important){
            Toast.makeText(getApplicationContext(), content, Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(getApplicationContext(), content, Toast.LENGTH_SHORT).show();
        }
    }

    private void initView() {
        anim_Rotate_Front = AnimationUtils.loadAnimation(this, R.anim.rotate_front);
        anim_Rotate_Back = AnimationUtils.loadAnimation(this, R.anim.rotate_back);

        controlView();
    }

    private void controlView() {
        binding.btnCapture.setOnClickListener(this);

        binding.btnFlash.setOnClickListener(this);
        binding.btnRotate.setOnClickListener(this);
        binding.btnGrid.setOnClickListener(this);
        binding.btnHdr.setOnClickListener(this);

//        btn_cap.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                try {
//                    Log.e(TAG, "onLongClick: btn_cap");
//                    camera.setMode(Mode.VIDEO);
////                    camera.takeVideo(videoDir);
//                    startTime = System.currentTimeMillis();
//                    handler.postDelayed(updateTimerThread, 0);
//                } catch(Exception e) {
//                    e.printStackTrace();
//                }
//
//                btn_Hdr.setVisibility(View.INVISIBLE);
//                btn_Grid.setVisibility(View.INVISIBLE);
//                btn_rotate.setVisibility(View.INVISIBLE);
//                btn_flash.setVisibility(View.INVISIBLE);
//                tv_Holdtap.setVisibility(View.INVISIBLE);
//                tv_timer.setVisibility(View.VISIBLE);
//
//                handler.postDelayed(scaleUpAnimation, 100);
//            }
//        });
    }

//    private void activeCameraCapture() {
//        btn_cap.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                btn_cap.setOnTouchListener(new View.OnTouchListener() {
//                    @Override
//                    public boolean onTouch(View view, MotionEvent motionEvent) {
//                        if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS){
//                            Log.e(TAG, "onTouch: ACTION_BUTTON_PRESS");
//                            capturePicture();
//                        }
//
//                        if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
//                            Log.e(TAG, "onTouch: ACTION_UP");
//                            camera.stopVideo();
//                            handler.postDelayed(scaleDownAnimation, 100);
//
//                            handler.removeCallbacks(updateTimerThread);
//                            tv_timer.setVisibility(View.INVISIBLE);
//
//                            btn_Grid.setVisibility(View.VISIBLE);
//                            btn_Hdr.setVisibility(View.VISIBLE);
//                            btn_flash.setVisibility(View.VISIBLE);
//                            btn_rotate.setVisibility(View.VISIBLE);
//                            tv_Holdtap.setVisibility(View.VISIBLE);
//
//                            camera.setMode(Mode.PICTURE);
//
//                            return true;
//                        }
//                        return true;
//                    }
//                });
//                return true;
//            }
//        });
//        return;
//    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_capture:
                capturePicture();
                break;
            case R.id.btn_rotate:
                rotateCamera();
                break;
            case R.id.btn_flash:
                flash();
                break;
            case R.id.btn_grid:
                grid();
                break;
            case R.id.btn_hdr:
                hdr();
                break;
        }
    }

    private void hdr() {
        if(binding.camera.getHdr() == Hdr.ON){
            binding.camera.setHdr(Hdr.OFF);
            binding.camera.setBackgroundResource(R.drawable.hdr_off_24dp);
            Log.e(TAG, "hdr: Off");
        } else{
            binding.camera.setHdr(Hdr.ON);
            binding.camera.setBackgroundResource(R.drawable.hdr_on_24dp);
            Log.e(TAG, "hdr: On");
        }
    }

    private final Runnable updateTimerThread = new Runnable() {
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            binding.tvTimer.setText(String.format("%d:%02d", minutes, seconds));

            handler.postDelayed(this, 500);
        }
    };

    private void rotateCamera() {
        if(binding.camera.isTakingVideo() || binding.camera.isTakingPicture()){
            Log.e(TAG, "rotateCamera: Taking Picture or Video");
            return;
        }
        switch (binding.camera.getFacing()) {
            case FRONT:
                binding.camera.setFacing(Facing.BACK);
                binding.btnRotate.startAnimation(anim_Rotate_Back);
                Log.e(TAG, "rotateCamera: Back");
                break;
            case BACK:
                binding.camera.setFacing(Facing.FRONT);
                binding.btnRotate.startAnimation(anim_Rotate_Front);
                Log.e(TAG, "rotateCamera: Front");
                break;
        }
    }

    private void flash() {
        if(binding.camera.isTakingPicture() || binding.camera.isTakingVideo()) { return; }
        switch (binding.camera.getFlash()) {
            case ON:
                binding.camera.setFlash(Flash.AUTO);
                binding.btnFlash.setBackgroundResource(R.drawable.ic_baseline_flash_auto_24);
                Log.e(TAG, "flash: Auto");
                break;
            case AUTO:
                binding.camera.setFlash(Flash.OFF);
                binding.btnFlash.setBackgroundResource(R.drawable.ic_baseline_flash_off_24);
                Log.e(TAG, "flash: Off");
                break;
            case OFF:
                binding.camera.setFlash(Flash.ON);
                binding.btnFlash.setBackgroundResource(R.drawable.ic_baseline_flash_on_24);
                Log.e(TAG, "flash: On");
                break;
        }
    }

    private void grid() {
        if (binding.camera.isTakingPicture() || binding.camera.isTakingVideo()) { return; }
        switch (binding.camera.getGrid()){
            case OFF:
                binding.camera.setGrid(Grid.DRAW_3X3);
                binding.btnGrid.setBackgroundResource(R.drawable.grid_on_24dp);
                Log.e(TAG, "changeGrid: 3*3");
                break;
            case DRAW_3X3:
                binding.camera.setGrid(Grid.OFF);
                binding.btnGrid.setBackgroundResource(R.drawable.grid_off_24dp);
                Log.e(TAG, "changeGrid: Off");
                break;
        }
    }

    public void capturePicture(){
        if (binding.camera.getMode() == Mode.VIDEO) {
            message("Can't take HQ pictures while in VIDEO mode.", false);
            return;
        }
        if (binding.camera.isTakingPicture()) { return; }
        try {
            binding.camera.takePicture();
        } catch (Exception e){
            Log.e(TAG, "capturePicture: Error Save Picture");
        }
    }

    private void captureVideo() {
        binding.camera.setMode(Mode.VIDEO);
        if (binding.camera.getMode() == Mode.PICTURE) {
            message("Can't record HQ videos while in PICTURE mode.", false);
            return;
        }
        if(binding.camera.isTakingVideo()) { return; }
        message("Recording for 5 seconds...", true);
//        camera.takeVideo(videoDir);
    }

    public String getImageString(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String encodeString = Base64.getEncoder().encodeToString(baos.toByteArray());
        return encodeString;
    }
}
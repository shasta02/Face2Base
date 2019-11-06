package com.recog.face2base;

        import android.Manifest;
        import android.app.Activity;
        import android.content.Context;
        import android.content.pm.PackageManager;
        import android.graphics.Rect;
        import android.hardware.Camera;
        import android.hardware.Camera.Face;
        import android.hardware.Camera.FaceDetectionListener;
        import android.hardware.SensorManager;
        import android.os.Bundle;
        import android.os.Vibrator;
        import android.support.v4.app.ActivityCompat;
        import android.support.v4.content.ContextCompat;
        import android.util.Log;
        import android.view.OrientationEventListener;
        import android.view.SurfaceHolder;
        import android.view.SurfaceView;
        import android.view.ViewGroup.LayoutParams;
        import android.widget.Toast;

        import java.util.ArrayList;
        import java.util.List;


public class CameraActivity extends Activity
        implements SurfaceHolder.Callback {

    public static final String TAG = CameraActivity.class.getSimpleName();

    private ArrayList<Double> lengthValues;
    private Camera mCamera;
    private Vibrator v;
    private long pastTime, thisTime;
    private int counter;

    // We need the phone orientation to correctly draw the overlay:
    private int mOrientation;
    private int mOrientationCompensation;
    private OrientationEventListener mOrientationEventListener;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;

    // Holds the Face Detection result:
    private Camera.Face[] mFaces;

    // The surface view for the camera data
    private SurfaceView mView;

    // Draw rectangles and other fancy stuff:
    private FaceOverlayView mFaceView;

    // Log all errors:
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    /**
     * Sets the faces for the overlay view, so it can be updated
     * and the face overlays will be drawn again.
     */
    private FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {
        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            counter++;
            thisTime = System.currentTimeMillis();
            if(faces.length>0){
                v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                //vibrate for x millis
                v.vibrate(500);
            }
            if(faces.length==0){
                lengthValues = new ArrayList<Double>();
                counter=0;
            }
            if(faces.length==1){
                Rect rect = new Rect();
                rect.set(faces[0].rect);
                double feetAway = (int)(Math.random()*5)*(0.75+(((100.0-(double)rect.width())/63)+((1400-(double)rect.height())/100))/24);

                /*
                if(pastTime-thisTime>500 && lengthValues.size()<=3) {
                    lengthValues.add(feetAway);
                    Log.d("onFaceDetection", "Feet away"+feetAway);
                    pastTime = thisTime;
                }
                else if(lengthValues.size()>3 && pastTime-thisTime>500){
                    lengthValues.add(feetAway);
                    long differences=0;
                    for(int i = 1; i<lengthValues.size(); i++){
                        Toast.makeText(CameraActivity.this,"In loop adding stuff", Toast.LENGTH_SHORT).show();
                        differences+=100*(lengthValues.get(i)-lengthValues.get(i-1));
                    }
                    Log.d("onFaceDetection", "Differences"+differences+"Denom: "+((lengthValues.size()-1)*(double)(thisTime-pastTime)/1000));
                    differences=(int)(differences/((lengthValues.size()-1)*(double)(thisTime-pastTime)/1000));
                   double time = 100*feetAway/differences;
                    Log.d("onFaceDetection", "Time in seconds to return "+time);
                    pastTime = thisTime;
                }
                */
            }
            for(int i =0; i<faces.length; i++) {
                Rect rect = new Rect();
                rect.set(faces[i].rect);
                Log.d("onFaceDetection", "Width of Rect: "+rect.width() +" Height: "+rect.height());

            }
            Log.d("onFaceDetection", "Number of Faces:" + faces.length);

            // Update the view now!
            mFaceView.setFaces(faces);

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mView = new SurfaceView(this);
        pastTime = System.currentTimeMillis();
        lengthValues = new ArrayList<Double>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            //ask for authorisation
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 50);
            recreate();
        }
        else
            //start your camera
            try {
                releaseCameraAndPreview();
                    mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

            } catch (Exception e) {
                Log.e(getString(R.string.app_name), "failed to open Camera");
                e.printStackTrace();
            }

        setContentView(mView);
        // Now create the OverlayView:
        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        // Create and Start the OrientationListener:
        mOrientationEventListener = new SimpleOrientationEventListener(this);
        mOrientationEventListener.enable();


    }

    private void releaseCameraAndPreview() {
       // mView.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
    }

    @Override
    protected void onPause() {
        mOrientationEventListener.disable();
        super.onPause();
    }

    @Override
    protected void onResume() {
        mOrientationEventListener.enable();
        super.onResume();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //mCamera = Camera.open();
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }
        mCamera.setFaceDetectionListener(faceDetectionListener);
        mCamera.startFaceDetection();
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }

        configureCamera(width, height);
        setDisplayOrientation();
        setErrorCallback();

        // Everything is configured! Finally start the camera preview again:
        mCamera.startPreview();
    }

    private void setErrorCallback() {
        mCamera.setErrorCallback(mErrorCallback);
    }

    private void setDisplayOrientation() {
        // Now set the display orientation:
        mDisplayRotation = Util.getDisplayRotation(CameraActivity.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, 0);

        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
    }

    private void configureCamera(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        // Set the PreviewSize and AutoFocus:
        setOptimalPreviewSize(parameters, width, height);
        setAutoFocus(parameters);
        // And set the parameters:
        mCamera.setParameters(parameters);
    }

    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        float targetRatio = (float) width / height;
        Camera.Size previewSize = Util.getOptimalPreviewSize(this, previewSizes, targetRatio);
        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallback(null);
        mCamera.setFaceDetectionListener(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }

    /**
     * We need to react on OrientationEvents to rotate the screen and
     * update the views.
     */
    private class SimpleOrientationEventListener extends OrientationEventListener {

        public SimpleOrientationEventListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            // We keep the last known orientation. So if the user first orient
            // the camera then point the camera to floor or sky, we still have
            // the correct orientation.
            if (orientation == ORIENTATION_UNKNOWN) return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            // When the screen is unlocked, display rotation may change. Always
            // calculate the up-to-date orientationCompensation.
            int orientationCompensation = mOrientation
                    + Util.getDisplayRotation(CameraActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                mFaceView.setOrientation(mOrientationCompensation);
            }
        }
    }
}
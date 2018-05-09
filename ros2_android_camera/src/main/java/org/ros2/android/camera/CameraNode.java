package org.ros2.android.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import org.ros2.android.activity.ROSActivity;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;
import org.ros2.rcljava.timer.WallTimer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.List;

class CameraNode extends BaseComposableNode {
  private final String TAG = this.getClass().getSimpleName();
  private final String mTopic;
  private Publisher<sensor_msgs.msg.Image> mPublisher;
  private final ROSActivity mActivity;
  private WallTimer mTimer;
  private ImageReader mImageReader;
  private CameraManager mCameraManager;
  private CameraDevice mCamera;
  private CameraCaptureSession mCaptureSession;
  private CaptureRequest.Builder mPreviewRequestBuilder;
  private final int mWidth = 320;
  private final int mHeight = 240;

  private final CameraDevice.StateCallback mCameraDeviceCallback
      = new CameraDevice.StateCallback() {

    @Override
    public void onOpened(CameraDevice camera) {
      mCamera = camera;
      Log.i(TAG, "onOpened");
      startCaptureSession();
    }

    @Override
    public void onClosed(CameraDevice camera) {
      Log.i(TAG, "onClosed");
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
      Log.i(TAG, "onDisconnected");
      mCamera = null;
    }

    @Override
    public void onError(CameraDevice camera, int error) {
      Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
      mCamera = null;
    }

  };

  private final CameraCaptureSession.StateCallback mSessionCallback
      = new CameraCaptureSession.StateCallback() {

    @Override
    public void onConfigured(CameraCaptureSession session) {
      if (mCamera == null) {
        return;
      }
      mCaptureSession = session;
      try {
        mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
            mCaptureCallback, null);
      } catch (CameraAccessException e) {
        Log.e(TAG, "Failed to start camera preview because it couldn't access camera", e);
      } catch (IllegalStateException e) {
        Log.e(TAG, "Failed to start camera preview.", e);
      }
    }

    @Override
    public void onConfigureFailed(CameraCaptureSession session) {
      Log.e(TAG, "Failed to configure capture session.");
    }

    @Override
    public void onClosed(CameraCaptureSession session) {
      if (mCaptureSession != null && mCaptureSession.equals(session)) {
        mCaptureSession = null;
      }
    }

  };

  CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

    @Override
    public void onCaptureStarted(CameraCaptureSession session,
                                 CaptureRequest request, long timestamp, long frameNumber) {
      // Temporary trampoline for API change transition
      Log.i(TAG, "onCaptureStarted(session, request, timestamp);");
    }

    @Override
    public void onCaptureCompleted(CameraCaptureSession session,
                                   CaptureRequest request, TotalCaptureResult result) {
      Log.i(TAG, "onCaptureCompleted(session, request, result);");
      try {
        Image img = mImageReader.acquireLatestImage();
        if (img != null) {
          final List<Byte> imageData = getListOfBytesFromImage(img);
          sensor_msgs.msg.Image msg = new sensor_msgs.msg.Image();
          msg.setData(imageData);
          msg.setIsBigendian((byte) 1);
          msg.setEncoding("rgba8");
          msg.setWidth(mWidth);
          msg.setHeight(mHeight);
          mPublisher.publish(msg);
          Log.i(TAG, "Image published");
          img.close();
        }
      } catch (IllegalStateException e) {
        Log.e(TAG, "Illegal state exception", e);
      }
    }
  };

  private static List<Byte> getListOfBytesFromImage(Image img) {
    final ByteBuffer buffer = img.getPlanes()[0].getBuffer(); //.rewind();

    byte[] bytes = new byte[buffer.remaining()];
    buffer.get(bytes);
    FileOutputStream output = null;
    File file = new File("/Pictures", "IMG_20180120_125000023.jpg");

    try {
      output = new FileOutputStream(file, false);
      output.write(bytes);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if(output != null);
      try {
        output.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    Bitmap bitmap = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Bitmap.Config.ARGB_8888);
    bitmap.copyPixelsFromBuffer(buffer);
    int heightWidth = img.getWidth() * img.getHeight();
    final ByteBuffer outBuffer = ByteBuffer.allocate(4 * heightWidth);
    bitmap.copyPixelsToBuffer(outBuffer);
    int listItemsCount = 4 * heightWidth;
    ArrayList<Byte> result = new ArrayList<Byte>(listItemsCount);

    for(int i = 0; i < listItemsCount; i++){
      result.add(outBuffer.get(i));
    }
    return result;
  }

  CameraNode(final ROSActivity activity, final String name, final String topic) {
    super(name);
    Log.i(TAG, "CameraNode()");
    mTopic = topic;
    mActivity = activity;
    mPublisher = this.node.createPublisher(sensor_msgs.msg.Image.class, mTopic);
  }

  void start() {
    Log.d(TAG, "start()");
    if (this.mTimer != null) {
      this.mTimer.cancel();
    }
    setupCamera();
   //  this.mTimer = node.createWallTimer(500, TimeUnit.MILLISECONDS, this::onTimer);
  }

  private void onTimer() {
    Surface surface = mImageReader.getSurface();
    if (surface == null || surface.isValid()) {
      Log.d(TAG, "onTimer: Surface is null on not valid");
    }

    Image img = mImageReader.acquireLatestImage();
    if (null == img) {
      Log.d(TAG, "onTimer: No image");
      return;
    }
    surface.release();

    Log.i(TAG, "onTimer: Image available");
    sensor_msgs.msg.Image msg = new sensor_msgs.msg.Image();
    msg.setEncoding("rgb8");
    msg.setWidth(mWidth);
    msg.setHeight(mHeight);
    this.mPublisher.publish(msg);
  }


  private void setupCamera() {
    Log.i(TAG, "setupCamera");
    mCameraManager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
    if (mCameraManager == null) {
      Log.e(TAG, "This device does not support Camera2 API");
      return;
    }
    try {
      String[] cameraIdList = mCameraManager.getCameraIdList();
      String cameraId = cameraIdList[0];
      Log.d(TAG, "CameraId" + cameraId);
      CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

      StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.JPEG, 3);

      mCameraManager.openCamera(cameraId, mCameraDeviceCallback, null);
    } catch (CameraAccessException e) {
      Log.e(TAG, "Problem with camera access", e);
    }
  }

  private void startCaptureSession() {
    Surface surface = mImageReader.getSurface();
    try {
      mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      mPreviewRequestBuilder.addTarget(surface);
      mCamera.createCaptureSession(Arrays.asList(surface), mSessionCallback, null);
    } catch (CameraAccessException e) {
      Log.e(TAG, "Problem with camera access", e);
    }
  }
}


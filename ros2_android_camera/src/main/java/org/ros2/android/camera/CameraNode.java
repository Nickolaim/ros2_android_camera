package org.ros2.android.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.util.Size;
import org.ros2.android.activity.ROSActivity;
import org.ros2.rcljava.node.BaseComposableNode;
import org.ros2.rcljava.publisher.Publisher;
import org.ros2.rcljava.timer.WallTimer;

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
  private final int mWidth = 320;
  private final int mHeight = 240;


  CameraNode(final ROSActivity activity, final String name, final String topic) {
    super(name);
    Log.i(TAG, "CameraNode()");
    mTopic = topic;
    mActivity = activity;
    this.node.createPublisher(sensor_msgs.msg.Image.class, mTopic);
  }

  void start() {
    Log.d(TAG, "start()");
    if (this.mTimer != null) {
      this.mTimer.cancel();
    }
    setupCamera();
    this.mTimer = node.createWallTimer(500, TimeUnit.MILLISECONDS, this::onTimer);
  }

  private void onTimer() {
    Image img = mImageReader.acquireLatestImage();
    if (null == img) {
      Log.d(TAG, "onTimer: No image");
      return;
    }

    Log.i(TAG, "onTimer: Image available");
    sensor_msgs.msg.Image msg = new sensor_msgs.msg.Image();
    msg.setEncoding("rgb8");
    msg.setWidth(mWidth);
    msg.setHeight(mHeight);
    this.mPublisher.publish(msg);
  }


  private void setupCamera() {
    Log.i(TAG, "setupCamera");
    CameraManager manager = (CameraManager) mActivity.getSystemService(Context.CAMERA_SERVICE);
    if (manager == null) {
      Log.e(TAG, "This device does not support Camera2 API");
      return;
    }
    try {
      String[] cameraIdList = manager.getCameraIdList();
      String cameraId = cameraIdList[0];
      Log.d(TAG, "CameraId" + cameraId);
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

      StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      mImageReader = ImageReader.newInstance(mWidth, mHeight, ImageFormat.JPEG, 3);
    } catch (CameraAccessException e) {
      Log.e(TAG, "Problem with camera access", e);
    }
  }
}

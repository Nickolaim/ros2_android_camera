package org.ros2.android.camera;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.util.Log;
import org.ros2.android.activity.ROSActivity;
import org.ros2.rcljava.RCLJava;

public class ROS2CameraActivity extends ROSActivity {
  private static final String RCL_JAVA_INIT_NEEDED = "RCL_JAVA_INIT_NEEDED";
  private final String TAG = this.getClass().getSimpleName();
  private static final int REQUEST_CAMERA_PERMISSIONS = 1;
  private boolean mRclJavaInitNeeded = true;

  @Override
  public final void onCreate(final Bundle savedInstanceState) {
    Log.i(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    if (savedInstanceState != null) {
      mRclJavaInitNeeded = savedInstanceState.getBoolean(RCL_JAVA_INIT_NEEDED, true);
    }

    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
      Log.e(TAG, "Camera permissions not granted");
      requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSIONS);
    } else {
      Log.i(TAG, "Camera permissions already granted");
    }

    if (mRclJavaInitNeeded) {
      Log.i(TAG, "rclJavaInit");
      RCLJava.rclJavaInit();
      mRclJavaInitNeeded = false;
    }

    CameraNode mCameraNode = new CameraNode(this, "camera_node", "image");
    getExecutor().addNode(mCameraNode);
    mCameraNode.start();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    if (outState != null) {
      outState.putBoolean(RCL_JAVA_INIT_NEEDED, mRclJavaInitNeeded);
    }
    super.onSaveInstanceState(outState);
  }
}

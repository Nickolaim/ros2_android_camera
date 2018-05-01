package org.ros2.android.camera;


import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import android.util.Log;
import org.ros2.android.activity.ROSActivity;

public class ROS2CameraActivity extends ROSActivity {
  private final String TAG = this.getClass().getSimpleName();

  @Override
  public final void onCreate(final Bundle savedInstanceState) {
    LogMessage("onCreate");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    if (manager == null) {
      LogMessage("This device does not support Camera2 API.");
    }
  }

  private void LogMessage(String message) {
    Log.i(TAG, message);
  }
}

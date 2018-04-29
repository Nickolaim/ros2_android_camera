package org.ros2.android.camera;


import android.os.Bundle;

import org.ros2.android.activity.ROSActivity;

public class ROS2CameraActivity extends ROSActivity {

  @Override
  public final void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
  }
}

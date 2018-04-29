package org.ros2.android.camera;

import org.ros2.rcljava.node.BaseComposableNode;

public class CameraNode extends BaseComposableNode {

  private final String topic;

  public CameraNode(final String name, final String topic) {
    super(name);
    this.topic = topic;
  }
}

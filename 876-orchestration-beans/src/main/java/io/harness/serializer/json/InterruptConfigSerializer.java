package io.harness.serializer.json;

import io.harness.pms.contracts.advisers.InterruptConfig;

public class InterruptConfigSerializer extends ProtoJsonSerializer<InterruptConfig> {
  public InterruptConfigSerializer() {
    super(InterruptConfig.class);
  }
}

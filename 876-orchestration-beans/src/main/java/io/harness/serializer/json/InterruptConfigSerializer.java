package io.harness.serializer.json;

import io.harness.pms.contracts.interrupts.InterruptConfig;

public class InterruptConfigSerializer extends ProtoJsonSerializer<InterruptConfig> {
  public InterruptConfigSerializer() {
    super(InterruptConfig.class);
  }
}

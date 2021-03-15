package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.advisers.InterruptConfig;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class InterruptConfigKryoSerializer extends ProtobufKryoSerializer<InterruptConfig> {
  private static InterruptConfigKryoSerializer instance;

  public InterruptConfigKryoSerializer() {}

  public static synchronized InterruptConfigKryoSerializer getInstance() {
    if (instance == null) {
      instance = new InterruptConfigKryoSerializer();
    }
    return instance;
  }
}

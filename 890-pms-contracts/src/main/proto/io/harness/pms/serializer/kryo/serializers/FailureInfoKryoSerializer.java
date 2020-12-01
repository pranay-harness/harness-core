package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.execution.failure.FailureInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class FailureInfoKryoSerializer extends ProtobufKryoSerializer<FailureInfo> {
  private static FailureInfoKryoSerializer instance;

  private FailureInfoKryoSerializer() {}

  public static synchronized FailureInfoKryoSerializer getInstance() {
    if (instance == null) {
      instance = new FailureInfoKryoSerializer();
    }
    return instance;
  }
}

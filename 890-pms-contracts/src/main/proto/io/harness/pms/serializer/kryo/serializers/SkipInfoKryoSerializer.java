package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class SkipInfoKryoSerializer extends ProtobufKryoSerializer<SkipInfo> {
  private static SkipInfoKryoSerializer instance;

  private SkipInfoKryoSerializer() {}

  public static synchronized SkipInfoKryoSerializer getInstance() {
    if (instance == null) {
      instance = new SkipInfoKryoSerializer();
    }
    return instance;
  }
}
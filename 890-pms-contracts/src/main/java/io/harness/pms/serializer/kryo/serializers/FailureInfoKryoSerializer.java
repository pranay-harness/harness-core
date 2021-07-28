package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
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

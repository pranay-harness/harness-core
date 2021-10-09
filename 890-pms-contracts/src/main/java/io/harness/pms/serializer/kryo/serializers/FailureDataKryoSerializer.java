package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class FailureDataKryoSerializer extends ProtobufKryoSerializer<FailureData> {
  private static FailureDataKryoSerializer instance;

  private FailureDataKryoSerializer() {}

  public static synchronized FailureDataKryoSerializer getInstance() {
    if (instance == null) {
      instance = new FailureDataKryoSerializer();
    }
    return instance;
  }
}

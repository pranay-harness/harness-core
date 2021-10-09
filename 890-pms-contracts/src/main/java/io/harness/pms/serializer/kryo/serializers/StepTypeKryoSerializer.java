package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class StepTypeKryoSerializer extends ProtobufKryoSerializer<StepType> {
  private static StepTypeKryoSerializer instance;

  private StepTypeKryoSerializer() {}

  public static synchronized StepTypeKryoSerializer getInstance() {
    if (instance == null) {
      instance = new StepTypeKryoSerializer();
    }
    return instance;
  }
}

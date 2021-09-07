package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class StepOutcomeRefKryoSerializer extends ProtobufKryoSerializer<StepOutcomeRef> {
  private static StepOutcomeRefKryoSerializer instance;

  private StepOutcomeRefKryoSerializer() {}

  public static synchronized StepOutcomeRefKryoSerializer getInstance() {
    if (instance == null) {
      instance = new StepOutcomeRefKryoSerializer();
    }
    return instance;
  }
}

package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.data.StepOutcomeRef;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

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

package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.plan.PartialPlanResponse;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class PartialPlanResponseKryoSerializer extends ProtobufKryoSerializer<PartialPlanResponse> {
  private static PartialPlanResponseKryoSerializer instance;

  private PartialPlanResponseKryoSerializer() {}

  public static synchronized PartialPlanResponseKryoSerializer getInstance() {
    if (instance == null) {
      instance = new PartialPlanResponseKryoSerializer();
    }
    return instance;
  }
}
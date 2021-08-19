package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.plan.PlanCreationBlobResponse;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class PlanCreationBlobResponseKryoSerializer extends ProtobufKryoSerializer<PlanCreationBlobResponse> {
  private static PlanCreationBlobResponseKryoSerializer instance;

  private PlanCreationBlobResponseKryoSerializer() {}

  public static synchronized PlanCreationBlobResponseKryoSerializer getInstance() {
    if (instance == null) {
      instance = new PlanCreationBlobResponseKryoSerializer();
    }
    return instance;
  }
}

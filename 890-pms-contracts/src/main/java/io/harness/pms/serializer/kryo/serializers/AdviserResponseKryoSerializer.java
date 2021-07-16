package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class AdviserResponseKryoSerializer extends ProtobufKryoSerializer<AdviserResponse> {
  private static AdviserResponseKryoSerializer instance;

  private AdviserResponseKryoSerializer() {}

  public static synchronized AdviserResponseKryoSerializer getInstance() {
    if (instance == null) {
      instance = new AdviserResponseKryoSerializer();
    }
    return instance;
  }
}

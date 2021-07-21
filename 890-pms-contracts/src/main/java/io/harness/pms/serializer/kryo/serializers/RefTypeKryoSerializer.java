package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.refobjects.RefType;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class RefTypeKryoSerializer extends ProtobufKryoSerializer<RefType> {
  private static RefTypeKryoSerializer instance;

  private RefTypeKryoSerializer() {}

  public static synchronized RefTypeKryoSerializer getInstance() {
    if (instance == null) {
      instance = new RefTypeKryoSerializer();
    }
    return instance;
  }
}

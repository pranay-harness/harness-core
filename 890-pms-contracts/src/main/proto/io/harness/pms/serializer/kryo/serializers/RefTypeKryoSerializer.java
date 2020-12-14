package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.refobjects.RefType;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

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

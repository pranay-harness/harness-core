package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class AmbianceKryoSerializer extends ProtobufKryoSerializer<Ambiance> {
  private static AmbianceKryoSerializer instance;

  private AmbianceKryoSerializer() {}

  public static synchronized AmbianceKryoSerializer getInstance() {
    if (instance == null) {
      instance = new AmbianceKryoSerializer();
    }
    return instance;
  }
}

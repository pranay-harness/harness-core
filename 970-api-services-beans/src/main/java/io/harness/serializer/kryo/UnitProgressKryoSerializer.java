package io.harness.serializer.kryo;

import io.harness.pms.contracts.ambiance.Ambiance;

public class UnitProgressKryoSerializer extends ProtobufKryoSerializer<Ambiance> {
  private static UnitProgressKryoSerializer instance;

  private UnitProgressKryoSerializer() {}

  public static synchronized UnitProgressKryoSerializer getInstance() {
    if (instance == null) {
      instance = new UnitProgressKryoSerializer();
    }
    return instance;
  }
}

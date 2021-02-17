package io.harness.serializer.kryo;

import io.harness.logging.UnitProgress;

public class UnitProgressKryoSerializer extends ProtobufKryoSerializer<UnitProgress> {
  private static UnitProgressKryoSerializer instance;

  private UnitProgressKryoSerializer() {}

  public static synchronized UnitProgressKryoSerializer getInstance() {
    if (instance == null) {
      instance = new UnitProgressKryoSerializer();
    }
    return instance;
  }
}

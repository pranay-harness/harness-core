package io.harness.logging.serializer.kryo;

import io.harness.logging.UnitProgress;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

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

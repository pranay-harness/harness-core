package io.harness.pms.serializer.kryo.serializers;

import io.harness.pms.ambiance.Level;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

public class LevelKryoSerializer extends ProtobufKryoSerializer<Level> {
  private static LevelKryoSerializer instance;

  private LevelKryoSerializer() {}

  public static synchronized LevelKryoSerializer getInstance() {
    if (instance == null) {
      instance = new LevelKryoSerializer();
    }
    return instance;
  }
}

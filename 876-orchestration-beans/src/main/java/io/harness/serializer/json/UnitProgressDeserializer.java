package io.harness.serializer.json;

import io.harness.logging.UnitProgress;

public class UnitProgressDeserializer extends ProtoJsonDeserializer<UnitProgress> {
  public UnitProgressDeserializer() {
    super(UnitProgress.class);
  }
}

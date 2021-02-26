package io.harness.serializer.json;

import io.harness.logging.UnitProgress;

public class UnitProgressSerializer extends ProtoJsonSerializer<UnitProgress> {
  public UnitProgressSerializer() {
    super(UnitProgress.class);
  }
}

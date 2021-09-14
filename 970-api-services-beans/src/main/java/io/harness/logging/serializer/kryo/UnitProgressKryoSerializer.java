/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.UnitProgress;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.DEL)
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

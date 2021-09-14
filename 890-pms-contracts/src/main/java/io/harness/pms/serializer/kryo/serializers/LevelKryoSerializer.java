/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
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

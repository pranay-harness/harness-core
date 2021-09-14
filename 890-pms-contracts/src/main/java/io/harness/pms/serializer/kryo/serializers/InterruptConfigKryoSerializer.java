/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.serializer.kryo.serializers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(HarnessTeam.PIPELINE)
public class InterruptConfigKryoSerializer extends ProtobufKryoSerializer<InterruptConfig> {
  private static InterruptConfigKryoSerializer instance;

  public InterruptConfigKryoSerializer() {}

  public static synchronized InterruptConfigKryoSerializer getInstance() {
    if (instance == null) {
      instance = new InterruptConfigKryoSerializer();
    }
    return instance;
  }
}

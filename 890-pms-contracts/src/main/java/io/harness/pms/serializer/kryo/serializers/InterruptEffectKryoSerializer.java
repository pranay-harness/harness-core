/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.serializer.kryo.serializers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.interrupts.InterruptEffectProto;
import io.harness.serializer.kryo.ProtobufKryoSerializer;

@OwnedBy(PIPELINE)
public class InterruptEffectKryoSerializer extends ProtobufKryoSerializer<InterruptEffectProto> {
  private static InterruptEffectKryoSerializer instance;

  public InterruptEffectKryoSerializer() {}

  public static synchronized InterruptEffectKryoSerializer getInstance() {
    if (instance == null) {
      instance = new InterruptEffectKryoSerializer();
    }
    return instance;
  }
}

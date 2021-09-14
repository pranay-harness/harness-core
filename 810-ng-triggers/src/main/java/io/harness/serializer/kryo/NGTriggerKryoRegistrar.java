/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PIPELINE)
public class NGTriggerKryoRegistrar implements KryoRegistrar {
  // Next ID: 400_002
  @Override
  public void register(Kryo kryo) {
    kryo.register(NGTriggerConfig.class, 400001);
    kryo.register(NGTriggerConfigV2.class, 400002);
  }
}

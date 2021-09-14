/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class OrchestrationBeansTestKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // Nothing to register
  }
}

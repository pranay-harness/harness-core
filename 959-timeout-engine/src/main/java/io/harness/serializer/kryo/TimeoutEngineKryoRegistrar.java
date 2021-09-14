/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import io.harness.serializer.KryoRegistrar;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;

import com.esotericsoftware.kryo.Kryo;

public class TimeoutEngineKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(AbsoluteTimeoutParameters.class, 9501);
  }
}

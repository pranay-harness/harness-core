/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import com.esotericsoftware.kryo.Kryo;

public interface KryoRegistrar {
  void register(Kryo kryo);
}

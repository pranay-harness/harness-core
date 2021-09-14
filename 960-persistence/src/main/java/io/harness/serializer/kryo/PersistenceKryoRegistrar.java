/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer.kryo;

import io.harness.beans.EmbeddedUser;
import io.harness.cache.VersionedKey;
import io.harness.serializer.KryoRegistrar;
import io.harness.springdata.exceptions.WingsTransactionFailureException;

import com.esotericsoftware.kryo.Kryo;

public class PersistenceKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(EmbeddedUser.class, 5021);
    kryo.register(VersionedKey.class, 5015);
    kryo.register(WingsTransactionFailureException.class, 96001);
  }
}

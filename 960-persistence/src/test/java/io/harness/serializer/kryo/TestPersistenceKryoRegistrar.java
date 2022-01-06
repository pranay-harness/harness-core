/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.cache.MongoStoreTestBase;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class TestPersistenceKryoRegistrar implements KryoRegistrar {
  int index = 21 * 10000;

  @Override
  public void register(Kryo kryo) {
    kryo.register(MongoStoreTestBase.TestNominalEntity.class, index++);
    kryo.register(MongoStoreTestBase.TestOrdinalEntity.class, index++);
  }
}

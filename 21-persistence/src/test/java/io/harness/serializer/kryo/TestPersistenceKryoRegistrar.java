package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cache.MongoStoreTest;
import io.harness.serializer.KryoRegistrar;

public class TestPersistenceKryoRegistrar implements KryoRegistrar {
  int index = 21 * 10000;

  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(MongoStoreTest.TestNominalEntity.class, index++);
    kryo.register(MongoStoreTest.TestOrdinalEntity.class, index++);
  }
}

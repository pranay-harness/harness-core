package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.cache.MongoStoreTestBase;
import io.harness.serializer.KryoRegistrar;

public class TestPersistenceKryoRegistrar implements KryoRegistrar {
  int index = 21 * 10000;

  @Override
  public void register(Kryo kryo) {
    kryo.register(MongoStoreTestBase.TestNominalEntity.class, index++);
    kryo.register(MongoStoreTestBase.TestOrdinalEntity.class, index++);
  }
}

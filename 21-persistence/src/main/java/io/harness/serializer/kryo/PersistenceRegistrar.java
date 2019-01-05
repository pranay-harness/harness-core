package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.beans.EmbeddedUser;
import io.harness.serializer.KryoRegistrar;

public class PersistenceRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) throws Exception {
    kryo.register(EmbeddedUser.class, 5021);
  }
}
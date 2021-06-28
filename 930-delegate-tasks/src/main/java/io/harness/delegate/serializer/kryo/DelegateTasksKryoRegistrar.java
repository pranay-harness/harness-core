package io.harness.delegate.serializer.kryo;

import io.harness.serializer.KryoRegistrar;

import software.wings.delegatetasks.cv.DataCollectionException;

import com.esotericsoftware.kryo.Kryo;

public class DelegateTasksKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(DataCollectionException.class, 7298);
  }
}

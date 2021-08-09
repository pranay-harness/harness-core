package io.harness.serializer;

import com.esotericsoftware.kryo.Kryo;

public interface KryoRegistrar {
  void register(Kryo kryo);
}

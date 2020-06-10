package io.harness.redis;

import com.esotericsoftware.kryo.Kryo;
import io.harness.serializer.KryoUtils;
import org.redisson.codec.KryoCodec;

import java.util.List;

public class RedissonKryoCodec extends KryoCodec {
  @Override
  protected Kryo createInstance(List<Class<?>> classes, ClassLoader classLoader) {
    return KryoUtils.kryo();
  }
}

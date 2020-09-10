package io.harness.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.esotericsoftware.kryo.Kryo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoUtils;
import org.redisson.codec.KryoCodec;

import java.util.List;

@OwnedBy(PL)
public class RedissonKryoCodec extends KryoCodec {
  @Override
  protected Kryo createInstance(List<Class<?>> classes, ClassLoader classLoader) {
    return KryoUtils.kryo();
  }
}

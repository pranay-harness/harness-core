package io.harness.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import io.harness.ng.core.common.beans.Tag;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.serializer.KryoRegistrar;

public class NGCoreKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Tag.class, 22001);
    kryo.register(ServiceEntity.class, 22002);
    kryo.register(Environment.class, 22003);
  }
}

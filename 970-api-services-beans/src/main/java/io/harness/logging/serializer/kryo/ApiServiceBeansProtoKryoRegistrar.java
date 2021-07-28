package io.harness.logging.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.DEL)
public class ApiServiceBeansProtoKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(UnitProgress.class, UnitProgressKryoSerializer.getInstance(), 9701);
    kryo.register(UnitStatus.class, 9702);
  }
}

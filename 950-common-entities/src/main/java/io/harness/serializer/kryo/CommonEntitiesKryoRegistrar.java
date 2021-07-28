package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.Permission;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(PL)
public class CommonEntitiesKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(EnvironmentType.class, 7398);
    kryo.register(Permission.class, 5310);
  }
}
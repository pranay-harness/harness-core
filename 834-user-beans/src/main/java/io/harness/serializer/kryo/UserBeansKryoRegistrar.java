package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.Role;
import software.wings.beans.RoleType;

import com.esotericsoftware.kryo.Kryo;

/**
 * Class will register all kryo classes
 */

@OwnedBy(HarnessTeam.PL)
public class UserBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(Role.class, 5194);
    kryo.register(RoleType.class, 5195);
  }
}

package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.user.UserMembershipUpdateMechanism;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PL)
public class NGCoreBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(EnvironmentType.class, 20100);
    kryo.register(DefaultExperience.class, 20101);
    kryo.register(UserMembershipUpdateMechanism.class, 20102);
    kryo.register(AuthenticationMechanism.class, 20103);
  }
}

package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authentication.AuthenticationMechanism;
import io.harness.beans.sso.SSOType;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PL)
public class SSOKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(SSOType.class, 5503);
    kryo.register(AuthenticationMechanism.class, 5357);
  }
}

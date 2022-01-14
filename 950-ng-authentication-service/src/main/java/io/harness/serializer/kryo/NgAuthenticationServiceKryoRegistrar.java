package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import software.wings.security.authentication.totp.RateLimitProtection;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.PL)
public class NgAuthenticationServiceKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(RateLimitProtection.class, 8075);
  }
}

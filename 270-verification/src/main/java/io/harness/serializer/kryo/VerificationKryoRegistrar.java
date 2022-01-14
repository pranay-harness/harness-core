package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CV)
public class VerificationKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to register
  }
}

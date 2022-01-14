package io.harness.serializer.kryo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(HarnessTeam.GITOPS)
public class GitOpsKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    // nothing to do
  }
}

package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.GitFileConfig;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDP)
public class CgOrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(GitFileConfig.class, 5472);
    // Put promoted classes here and do not change the id
  }
}

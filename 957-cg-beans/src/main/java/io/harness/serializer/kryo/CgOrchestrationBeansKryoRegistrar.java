package io.harness.serializer.kryo;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.context.ContextElementType;
import io.harness.serializer.KryoRegistrar;

import software.wings.api.ContainerServiceData;
import software.wings.beans.GitFileConfig;
import software.wings.beans.LicenseInfo;

import com.esotericsoftware.kryo.Kryo;

@OwnedBy(CDP)
public class CgOrchestrationBeansKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(ContextElementType.class, 4004);
    kryo.register(GitFileConfig.class, 5472);
    kryo.register(LicenseInfo.class, 5511);

    // Put promoted classes here and do not change the id
    kryo.register(ContainerServiceData.class, 5157);
  }
}

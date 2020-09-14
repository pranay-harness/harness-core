package io.harness.serializer.kryo;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.KryoRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConnectorNextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(NGCoreKryoRegistrar.class)
          .add(NGCoreBeansKryoRegistrar.class)
          .add(DelegateTasksBeansKryoRegister.class)
          .add(ProjectAndOrgKryoRegistrar.class)
          .add(ApiServiceBeansKryoRegister.class)
          .add(SecretManagerClientKryoRegistrar.class)
          .add(WaitEngineKryoRegister.class)
          .build();
}

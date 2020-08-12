package io.harness.serializer.morphia;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.ApiServiceBeansKryoRegister;
import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import io.harness.serializer.kryo.NGCoreBeansKryoRegistrar;
import io.harness.serializer.kryo.NGCoreKryoRegistrar;
import io.harness.serializer.kryo.ProjectAndOrgKryoRegistrar;
import io.harness.serializer.kryo.SecretManagerClientKryoRegistrar;
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
          .build();
}

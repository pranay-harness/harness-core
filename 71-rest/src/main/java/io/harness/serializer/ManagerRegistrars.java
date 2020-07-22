package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar;
import io.harness.serializer.kryo.DelegateAgentBeansKryoRegister;
import io.harness.serializer.kryo.DelegateAgentKryoRegister;
import io.harness.serializer.kryo.ManagerKryoRegistrar;
import io.harness.serializer.kryo.NGCoreKryoRegistrar;
import io.harness.serializer.kryo.SecretManagerClientKryoRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ManagerRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(ManagerKryoRegistrar.class)
          .add(SecretManagerClientKryoRegistrar.class)
          .add(NGCoreKryoRegistrar.class)
          .add(CvNextGenCommonsBeansKryoRegistrar.class)
          // temporary:
          .add(DelegateAgentKryoRegister.class)
          .add(DelegateAgentBeansKryoRegister.class)
          .build();
}

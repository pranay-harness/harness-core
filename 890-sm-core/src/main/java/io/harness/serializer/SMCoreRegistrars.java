package io.harness.serializer;

import io.harness.delegate.serializer.DelegateTasksRegistrars;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.SMCoreKryoRegistrar;
import io.harness.serializer.morphia.SMCoreMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SMCoreRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(SecretManagerClientRegistrars.kryoRegistrars)
          .addAll(RbacCoreRegistrars.kryoRegistrars)
          .addAll(ConnectorBeansRegistrars.kryoRegistrars)
          .add(SMCoreKryoRegistrar.class)
          .build();
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(SecretManagerClientRegistrars.morphiaRegistrars)
          .addAll(RbacCoreRegistrars.morphiaRegistrars)
          .addAll(DelegateTasksRegistrars.morphiaRegistrars)
          .add(SMCoreMorphiaRegistrar.class)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .build();
}

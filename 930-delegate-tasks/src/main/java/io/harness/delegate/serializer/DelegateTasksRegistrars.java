package io.harness.delegate.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.ApiServicesRegistrars;
import io.harness.serializer.CapabilityRegistrars;
import io.harness.serializer.DelegateTasksBeansRegistrars;
import io.harness.serializer.KryoRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateTasksRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ApiServicesRegistrars.kryoRegistrars)
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ApiServicesRegistrars.morphiaRegistrars)
          .addAll(CapabilityRegistrars.morphiaRegistrars)
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .build();
}

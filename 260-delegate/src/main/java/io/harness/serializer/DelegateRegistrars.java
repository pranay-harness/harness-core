package io.harness.serializer;

import io.harness.serializer.kryo.CvNextGenCommonsBeansKryoRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ManagerRegistrars.kryoRegistrars)
          .add(CvNextGenCommonsBeansKryoRegistrar.class)
          .addAll(CvNextGenCommonsRegistrars.kryoRegistrars)
          .build();
}

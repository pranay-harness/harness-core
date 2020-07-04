package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.CIExecutionRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CiExecutionRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CiBeansRegistrars.kryoRegistrars)
          .add(CIExecutionRegistrar.class)
          .build();
}

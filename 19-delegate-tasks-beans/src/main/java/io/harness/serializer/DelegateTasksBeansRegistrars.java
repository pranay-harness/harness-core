package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.DelegateTasksBeansKryoRegister;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DelegateTasksBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ApiServiceBeansRegistrars.kryoRegistrars)
          .add(DelegateTasksBeansKryoRegister.class)
          .build();
}

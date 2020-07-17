package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.DelegateTasksBeansRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.PersistenceRegistrars;
import io.harness.serializer.kryo.WaitEngineKryoRegister;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WaitEngineRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(PersistenceRegistrars.kryoRegistrars)
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .add(WaitEngineKryoRegister.class)
          .build();
}

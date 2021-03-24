package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.PMSPipelineMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;

public class ChangeDataCaptureRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(ManagerRegistrars.kryoRegistrars).build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ManagerRegistrars.morphiaRegistrars)
          .add(PMSPipelineMorphiaRegistrar.class)
          .build();
}

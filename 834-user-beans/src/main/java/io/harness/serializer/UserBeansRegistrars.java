package io.harness.serializer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGPipelineKryoRegistrar;
import io.harness.serializer.kryo.UserBeansKryoRegistrar;
import io.harness.serializer.morphia.UserBeansMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class UserBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(UserBeansKryoRegistrar.class)
          .add(NGPipelineKryoRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(UserBeansMorphiaRegistrar.class).build();
}

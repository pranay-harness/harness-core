package io.harness.serializer.registrars;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.CommonsRegistrars;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.kryo.NGCommonsKryoRegistrar;
import io.harness.serializer.morphia.NGCommonsMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGCommonsRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CommonsRegistrars.kryoRegistrars)
          .add(NGCommonsKryoRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(NGCommonsMorphiaRegistrar.class).build();
}

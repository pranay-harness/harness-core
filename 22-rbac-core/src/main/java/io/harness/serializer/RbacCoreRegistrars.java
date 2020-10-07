package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.RbacCoreKryoRegistrar;
import io.harness.serializer.morphia.RbacCoreMorphiaRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RbacCoreRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder().add(RbacCoreKryoRegistrar.class).build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder().add(RbacCoreMorphiaRegistrar.class).build();
}

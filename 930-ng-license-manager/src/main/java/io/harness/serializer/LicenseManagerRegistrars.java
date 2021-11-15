package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.LicenseManagerKryoRegistrar;
import io.harness.serializer.morphia.LicenseManagerMorphiaClassesRegistrar;

import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LicenseManagerRegistrars {
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(LicenseManagerMorphiaClassesRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .addAll(LicenseBeanRegistrar.kryoRegistrars)
          .add(LicenseManagerKryoRegistrar.class)
          .build();
}

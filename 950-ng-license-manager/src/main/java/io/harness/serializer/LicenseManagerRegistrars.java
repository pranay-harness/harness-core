package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.morphia.LicenseManagerMorphiaClassesRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LicenseManagerRegistrars {
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(LicenseManagerMorphiaClassesRegistrar.class)
          .build();
}

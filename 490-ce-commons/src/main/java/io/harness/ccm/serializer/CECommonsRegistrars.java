package io.harness.ccm.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.ccm.serializer.morphia.CECommonsMorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.CommonsRegistrars;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CECommonsRegistrars {
  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CommonsRegistrars.morphiaRegistrars)
          .add(CECommonsMorphiaRegistrar.class)
          .build();
}

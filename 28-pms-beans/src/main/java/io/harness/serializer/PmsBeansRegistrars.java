package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.spring.PmsBeansAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PmsBeansRegistrars {
  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder().add(PmsBeansAliasRegistrar.class).build();
}

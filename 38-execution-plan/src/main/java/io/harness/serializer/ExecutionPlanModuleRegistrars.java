package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.spring.ExecutionPlanAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionPlanModuleRegistrars {
  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder().add(ExecutionPlanAliasRegistrar.class).build();
}

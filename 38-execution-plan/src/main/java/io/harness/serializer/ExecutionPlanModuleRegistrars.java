package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.serializer.kryo.ExecutionPlanKryoRegistrar;
import io.harness.serializer.spring.ExecutionPlanAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionPlanModuleRegistrars {
  public final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .add(ExecutionPlanAliasRegistrar.class)
          .addAll(YamlBeansModuleRegistrars.aliasRegistrars)
          .build();

  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(ExecutionPlanKryoRegistrar.class)
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .build();
}

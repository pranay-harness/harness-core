package io.harness.serializer;

import com.google.common.collect.ImmutableSet;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CIBeansKryoRegistrar;
import io.harness.serializer.morphia.CIBeansMorphiaRegistrar;
import io.harness.serializer.spring.CIBeansAliasRegistrar;
import io.harness.spring.AliasRegistrar;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CiBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ExecutionPlanModuleRegistrars.kryoRegistrars)
          .addAll(ManagerRegistrars.kryoRegistrars)
          .addAll(NGPipelineRegistrars.kryoRegistrars)
          .add(CIBeansKryoRegistrar.class)
          .addAll(CvNextGenCommonsRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ExecutionPlanModuleRegistrars.morphiaRegistrars)
          .addAll(CvNextGenCommonsRegistrars.morphiaRegistrars)
          .addAll(ManagerRegistrars.morphiaRegistrars)
          .add(CIBeansMorphiaRegistrar.class)
          .addAll(NGPipelineRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(ExecutionPlanModuleRegistrars.aliasRegistrars)
          .addAll(CvNextGenCommonsRegistrars.aliasRegistrars)
          .addAll(NGPipelineRegistrars.aliasRegistrars)
          .addAll(ManagerRegistrars.aliasRegistrars)
          .add(CIBeansAliasRegistrar.class)
          .build();
}

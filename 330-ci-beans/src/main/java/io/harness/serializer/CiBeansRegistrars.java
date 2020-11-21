package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.CIBeansKryoRegistrar;
import io.harness.serializer.morphia.CIBeansMorphiaRegistrar;
import io.harness.serializer.morphia.YamlMorphiaRegistrar;
import io.harness.serializer.spring.CIBeansAliasRegistrar;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CiBeansRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(ExecutionPlanModuleRegistrars.kryoRegistrars)
          .addAll(NGPipelineRegistrars.kryoRegistrars)
          .addAll(ProjectAndOrgRegistrars.kryoRegistrars)
          .addAll(NGCoreBeansRegistrars.kryoRegistrars)
          .addAll(SecretManagerClientRegistrars.kryoRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.kryoRegistrars)
          .add(CIBeansKryoRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(ExecutionPlanModuleRegistrars.morphiaRegistrars)
          .addAll(NGPipelineRegistrars.morphiaRegistrars)
          .addAll(ProjectAndOrgRegistrars.morphiaRegistrars)
          .addAll(NGCoreBeansRegistrars.morphiaRegistrars)
          .addAll(SecretManagerClientRegistrars.morphiaRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.morphiaRegistrars)
          .add(CIBeansMorphiaRegistrar.class)
          .add(YamlMorphiaRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(ExecutionPlanModuleRegistrars.aliasRegistrars)
          .addAll(NGPipelineRegistrars.aliasRegistrars)
          .addAll(ProjectAndOrgRegistrars.aliasRegistrars)
          .addAll(NGCoreBeansRegistrars.aliasRegistrars)
          .addAll(SecretManagerClientRegistrars.aliasRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.aliasRegistrars)
          .addAll(YamlBeansModuleRegistrars.aliasRegistrars)
          .add(CIBeansAliasRegistrar.class)
          .build();
}

package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.NGPipelineKryoRegistrar;
import io.harness.serializer.morphia.NGPipelineMorphiaRegistrar;
import io.harness.serializer.spring.NGPipelineAliasRegistrar;
import io.harness.spring.AliasRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGPipelineRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.kryoRegistrars)
          .addAll(ProjectAndOrgRegistrars.kryoRegistrars)
          .addAll(NGCoreClientRegistrars.kryoRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.kryoRegistrars)
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .add(NGPipelineKryoRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends AliasRegistrar>> aliasRegistrars =
      ImmutableSet.<Class<? extends AliasRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.aliasRegistrars)
          .addAll(ProjectAndOrgRegistrars.aliasRegistrars)
          .addAll(NGCoreClientRegistrars.aliasRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.aliasRegistrars)
          .addAll(YamlBeansModuleRegistrars.aliasRegistrars)
          .add(NGPipelineAliasRegistrar.class)
          .build();

  public final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(DelegateTasksBeansRegistrars.morphiaRegistrars)
          .addAll(ProjectAndOrgRegistrars.morphiaRegistrars)
          .addAll(NGCoreClientRegistrars.morphiaRegistrars)
          .addAll(OrchestrationVisualizationModuleRegistrars.morphiaRegistrars)
          .addAll(YamlBeansModuleRegistrars.morphiaRegistrars)
          .add(NGPipelineMorphiaRegistrar.class)
          .build();
}

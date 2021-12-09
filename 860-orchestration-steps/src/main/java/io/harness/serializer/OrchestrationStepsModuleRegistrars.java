package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.plancreator.pipeline.PipelineConfig;
import io.harness.plancreator.steps.http.HttpStepNode;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.serializer.kryo.CommonEntitiesKryoRegistrar;
import io.harness.serializer.kryo.DelegateServiceBeansKryoRegistrar;
import io.harness.serializer.kryo.NGCoreBeansKryoRegistrar;
import io.harness.serializer.kryo.OrchestrationStepsKryoRegistrar;
import io.harness.serializer.kryo.YamlKryoRegistrar;
import io.harness.serializer.morphia.NotificationClientRegistrars;
import io.harness.serializer.morphia.OrchestrationStepsMorphiaRegistrar;
import io.harness.steps.approval.stage.ApprovalStageConfig;
import io.harness.steps.shellscript.ShellScriptStepNode;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
@OwnedBy(PIPELINE)
public class OrchestrationStepsModuleRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(OrchestrationRegistrars.kryoRegistrars)
          .add(OrchestrationStepsKryoRegistrar.class)
          .add(PipelineServiceUtilKryoRegistrar.class)
          .add(YamlKryoRegistrar.class)
          .add(NGCoreBeansKryoRegistrar.class)
          .add(DelegateServiceBeansKryoRegistrar.class)
          .add(CommonEntitiesKryoRegistrar.class)
          .addAll(PmsCommonsModuleRegistrars.kryoRegistrars)
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .addAll(NotificationClientRegistrars.kryoRegistrars)
          .addAll(RbacCoreRegistrars.kryoRegistrars)
          .addAll(NGCoreClientRegistrars.kryoRegistrars)
          .addAll(DelegateTaskRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(OrchestrationRegistrars.morphiaRegistrars)
          .add(OrchestrationStepsMorphiaRegistrar.class)
          .addAll(YamlBeansModuleRegistrars.morphiaRegistrars)
          .addAll(NotificationClientRegistrars.morphiaRegistrars)
          .addAll(RbacCoreRegistrars.morphiaRegistrars)
          .addAll(NGCoreClientRegistrars.morphiaRegistrars)
          .addAll(DelegateTaskRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(OrchestrationBeansRegistrars.morphiaConverters)
          .build();

  public static final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.<Class<? extends Converter<?, ?>>>builder()
          .addAll(OrchestrationRegistrars.springConverters)
          .build();

  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.PIPELINES)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(PipelineConfig.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.PIPELINE_STEPS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(PMSStepInfo.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.APPROVAL_STAGE)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(ApprovalStageConfig.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.FEATURE_FLAG_STAGE)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(ApprovalStageConfig.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.HTTP_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(HttpStepNode.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.SHELL_SCRIPT_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(ShellScriptStepNode.class)
                   .build())
          .build();
}

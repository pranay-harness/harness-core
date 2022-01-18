/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.serializer;

import io.harness.EntityType;
import io.harness.ModuleType;
import io.harness.accesscontrol.serializer.AccessControlClientRegistrars;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.core.beans.CVVerifyStepNode;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.serializer.kryo.PmsContractsKryoRegistrar;
import io.harness.serializer.kryo.CVNGKryoRegistrar;
import io.harness.serializer.morphia.CVNextGenMorphiaRegister;
import io.harness.serializer.morphia.NotificationClientRegistrars;
import io.harness.yaml.schema.beans.YamlGroup;
import io.harness.yaml.schema.beans.YamlSchemaMetadata;
import io.harness.yaml.schema.beans.YamlSchemaRootClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.converters.TypeConverter;

@UtilityClass
@OwnedBy(HarnessTeam.CV)
public class CvNextGenRegistrars {
  public static final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(CvNextGenCommonsRegistrars.kryoRegistrars)
          .addAll(ConnectorNextGenRegistrars.kryoRegistrars)
          .add(CVNGKryoRegistrar.class)
          .add(PipelineServiceUtilKryoRegistrar.class)
          .add(PmsContractsKryoRegistrar.class)
          .addAll(NotificationClientRegistrars.kryoRegistrars)
          .addAll(OrchestrationBeansRegistrars.kryoRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.kryoRegistrars)
          .addAll(AccessControlClientRegistrars.kryoRegistrars)
          .addAll(DelegateTaskRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(CvNextGenCommonsRegistrars.morphiaRegistrars)
          .add(CVNextGenMorphiaRegister.class)
          .addAll(NotificationClientRegistrars.morphiaRegistrars)
          .addAll(ConnectorBeansRegistrars.morphiaRegistrars)
          .addAll(OrchestrationBeansRegistrars.morphiaRegistrars)
          .addAll(OrchestrationStepsModuleRegistrars.morphiaRegistrars)
          .addAll(PrimaryVersionManagerRegistrars.morphiaRegistrars)
          .addAll(DelegateTaskRegistrars.morphiaRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends TypeConverter>> morphiaConverters =
      ImmutableSet.<Class<? extends TypeConverter>>builder()
          .addAll(PersistenceRegistrars.morphiaConverters)
          .addAll(OrchestrationBeansRegistrars.morphiaConverters)
          .build();
  public static final ImmutableList<YamlSchemaRootClass> yamlSchemaRegistrars =
      ImmutableList.<YamlSchemaRootClass>builder()
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.DEPLOYMENT_STEPS)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(StepElementConfig.class)
                   .build())
          .add(YamlSchemaRootClass.builder()
                   .entityType(EntityType.VERIFY_STEP)
                   .availableAtProjectLevel(true)
                   .availableAtOrgLevel(false)
                   .availableAtAccountLevel(false)
                   .clazz(CVVerifyStepNode.class)
                   .yamlSchemaMetadata(YamlSchemaMetadata.builder()
                                           .modulesSupported(Collections.singletonList(ModuleType.CD))
                                           .yamlGroup(YamlGroup.builder().group(StepCategory.STEP.name()).build())
                                           .build())
                   .build())
          .build();
}

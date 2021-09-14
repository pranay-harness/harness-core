/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.serializer.GitSyncSdkRegistrar;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.PmsCommonsKryoRegistrar;
import io.harness.serializer.morphia.PmsCommonsMorphiaRegistrar;
import io.harness.serializer.spring.converters.ambiance.AmbianceReadConverter;
import io.harness.serializer.spring.converters.ambiance.AmbianceWriteConverter;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseReadConverter;
import io.harness.serializer.spring.converters.facilitators.response.FacilitatorResponseWriteConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionReadConverter;
import io.harness.serializer.spring.converters.nodeexecution.NodeExecutionWriteConverter;
import io.harness.serializer.spring.converters.orchestrationMap.OrchestrationMapReadConverter;
import io.harness.serializer.spring.converters.orchestrationMap.OrchestrationMapWriteConverter;
import io.harness.serializer.spring.converters.outcomes.PmsOutcomeReadConverter;
import io.harness.serializer.spring.converters.outcomes.PmsOutcomeWriteConverter;
import io.harness.serializer.spring.converters.outputs.PmsSweepingOutputReadConverter;
import io.harness.serializer.spring.converters.outputs.PmsSweepingOutputWriteConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoReadConverter;
import io.harness.serializer.spring.converters.plannode.PlanNodeProtoWriteConverter;
import io.harness.serializer.spring.converters.sdk.SdkModuleInfoReadConverter;
import io.harness.serializer.spring.converters.sdk.SdkModuleInfoWriteConverter;
import io.harness.serializer.spring.converters.stepdetails.PmsStepDetailsReadConverter;
import io.harness.serializer.spring.converters.stepdetails.PmsStepDetailsWriteConverter;
import io.harness.serializer.spring.converters.stepparameters.PmsStepParametersReadConverter;
import io.harness.serializer.spring.converters.stepparameters.PmsStepParametersWriteConverter;
import io.harness.serializer.spring.converters.steptype.StepTypeReadConverter;
import io.harness.serializer.spring.converters.steptype.StepTypeWriteConverter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.serializer.registrars.NGCommonsRegistrars;
import lombok.experimental.UtilityClass;
import org.springframework.core.convert.converter.Converter;

@UtilityClass
@OwnedBy(PIPELINE)
public class PmsCommonsModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .add(PmsCommonsKryoRegistrar.class)
          .addAll(YamlBeansModuleRegistrars.kryoRegistrars)
          .addAll(NGCommonsRegistrars.kryoRegistrars)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .add(PmsCommonsMorphiaRegistrar.class)
          .addAll(NGCommonsRegistrars.morphiaRegistrars)
          .addAll(GitSyncSdkRegistrar.morphiaRegistrars)
          .build();

  public final ImmutableList<Class<? extends Converter<?, ?>>> springConverters =
      ImmutableList.of(StepTypeReadConverter.class, StepTypeWriteConverter.class, AmbianceWriteConverter.class,
          AmbianceReadConverter.class, FacilitatorResponseReadConverter.class, FacilitatorResponseWriteConverter.class,
          PlanNodeProtoReadConverter.class, PlanNodeProtoWriteConverter.class, NodeExecutionReadConverter.class,
          NodeExecutionWriteConverter.class, SdkModuleInfoReadConverter.class, SdkModuleInfoWriteConverter.class,
          OrchestrationMapReadConverter.class, OrchestrationMapWriteConverter.class, PmsOutcomeReadConverter.class,
          PmsOutcomeWriteConverter.class, PmsSweepingOutputReadConverter.class, PmsSweepingOutputWriteConverter.class,
          PmsStepParametersReadConverter.class, PmsStepParametersWriteConverter.class,
          PmsStepDetailsReadConverter.class, PmsStepDetailsWriteConverter.class);
}

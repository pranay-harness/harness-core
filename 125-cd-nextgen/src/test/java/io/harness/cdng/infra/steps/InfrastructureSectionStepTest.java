/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.infra.steps;

import static io.harness.rule.OwnerRule.ACHYUTH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.contracts.plan.ExecutionPrincipalInfo;
import io.harness.pms.contracts.plan.PrincipalType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.tasks.ResponseData;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class InfrastructureSectionStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock EnvironmentService environmentService;
  @InjectMocks private InfrastructureSectionStep infrastructureSectionStep;

  @Mock ExecutionSweepingOutputService executionSweepingOutputService;
  @Mock AccessControlClient accessControlClient;

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testValidateResourcesNullEnvRef() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setMetadata(ExecutionMetadata.newBuilder()
                                             .setPrincipalInfo(ExecutionPrincipalInfo.newBuilder()
                                                                   .setPrincipalType(PrincipalType.USER)
                                                                   .setPrincipal("Principal")
                                                                   .build())
                                             .build())
                            .build();
    InfraSectionStepParameters stepParameters = InfraSectionStepParameters.builder().build();
    doNothing().when(accessControlClient).checkForAccessOrThrow(any(), any(), any(), any());

    assertThatCode(() -> infrastructureSectionStep.validateResources(ambiance, stepParameters))
        .doesNotThrowAnyException();
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testValidateResourcesWithEnvRef() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setMetadata(ExecutionMetadata.newBuilder()
                                             .setPrincipalInfo(ExecutionPrincipalInfo.newBuilder()
                                                                   .setPrincipalType(PrincipalType.USER)
                                                                   .setPrincipal("Principal")
                                                                   .build())
                                             .build())
                            .build();
    InfraSectionStepParameters stepParameters =
        InfraSectionStepParameters.builder().environmentRef(ParameterField.createValueField("envRef")).build();

    infrastructureSectionStep.validateResources(ambiance, stepParameters);
    verify(accessControlClient, times(1)).checkForAccessOrThrow(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testObtainChildAfterRbac() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setMetadata(ExecutionMetadata.newBuilder()
                                             .setPrincipalInfo(ExecutionPrincipalInfo.newBuilder()
                                                                   .setPrincipalType(PrincipalType.USER)
                                                                   .setPrincipal("Principal")
                                                                   .build())
                                             .build())
                            .build();
    InfraSectionStepParameters stepParameters = InfraSectionStepParameters.builder()
                                                    .childNodeID("123")
                                                    .environmentRef(ParameterField.createValueField("envRef"))
                                                    .build();

    StepInputPackage stepInputPackage = StepInputPackage.builder().build();

    Optional<Environment> optionalEnvironment = Optional.of(Environment.builder().build());
    when(executionSweepingOutputService.consume(any(), any(), any(), any())).thenReturn("xyz");
    when(environmentService.get(any(), any(), any(), any(), anyBoolean())).thenReturn(optionalEnvironment);

    assertThat(infrastructureSectionStep.obtainChildAfterRbac(ambiance, stepParameters, stepInputPackage)).isNotNull();
    verify(executionSweepingOutputService, times(1)).consume(any(), any(), any(), any());
    verify(environmentService, times(1)).get(any(), any(), any(), any(), anyBoolean());
  }

  @Test
  @Owner(developers = ACHYUTH)
  @Category(UnitTests.class)
  public void testHandleChildResponse() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setMetadata(ExecutionMetadata.newBuilder()
                                             .setPrincipalInfo(ExecutionPrincipalInfo.newBuilder()
                                                                   .setPrincipalType(PrincipalType.USER)
                                                                   .setPrincipal("Principal")
                                                                   .build())
                                             .build())
                            .build();
    InfraSectionStepParameters stepParameters = InfraSectionStepParameters.builder()
                                                    .childNodeID("123")
                                                    .environmentRef(ParameterField.createValueField("envRef"))
                                                    .build();
    Map<String, ResponseData> responseDataMap = new HashMap<>();
    StepResponseNotifyData stepResponseNotifyData = StepResponseNotifyData.builder().build();
    responseDataMap.put("abc", stepResponseNotifyData);

    assertThat(infrastructureSectionStep.handleChildResponse(ambiance, stepParameters, responseDataMap)).isNotNull();
  }
}

/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.terragrunt;

import static io.harness.rule.OwnerRule.VLICA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.InlineFileConfig;
import io.harness.delegate.beans.storeconfig.InlineStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.beans.terragrunt.request.TerragruntApplyTaskParameters;
import io.harness.delegate.beans.terragrunt.request.TerragruntTaskRunType;
import io.harness.delegate.beans.terragrunt.response.TerragruntApplyTaskResponse;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({StepUtils.class})
@OwnedBy(HarnessTeam.CDP)
public class TerragruntApplyStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private KryoSerializer kryoSerializer;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock private TerragruntStepHelper terragruntStepHelper;
  @Mock private PipelineRbacHelper pipelineRbacHelper;
  @Mock private StepHelper stepHelper;
  @InjectMocks private TerragruntApplyStep terragruntApplyStep = new TerragruntApplyStep();
  @Captor ArgumentCaptor<List<EntityDetail>> captor;

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testValidateResourcesWhenApplyInline() {
    TerragruntConfigFilesWrapper configFilesWrapper = TerragruntTestStepUtils.createConfigFilesWrapper();
    LinkedHashMap<String, TerragruntVarFile> varFilesMap = TerragruntTestStepUtils.createVarFilesRemote();
    TerragruntBackendConfig terragruntBackendConfig = TerragruntTestStepUtils.createRemoteBackendConfig();

    TerragruntApplyStepParameters parameters =
        TerragruntApplyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(TerragruntStepConfigurationParameters.builder()
                               .type(TerragruntStepConfigurationType.INLINE)
                               .spec(TerragruntExecutionDataParameters.builder()
                                         .configFiles(configFilesWrapper)
                                         .backendConfig(terragruntBackendConfig)
                                         .varFiles(varFilesMap)
                                         .build())
                               .build())
            .build();

    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(parameters).build();
    terragruntApplyStep.validateResources(TerragruntTestStepUtils.getAmbiance(), stepElementParameters);
    verify(pipelineRbacHelper, times(1))
        .checkRuntimePermissions(eq(TerragruntTestStepUtils.getAmbiance()), captor.capture(), eq(true));

    List<EntityDetail> entityDetails = captor.getValue();
    assertThat(entityDetails.size()).isEqualTo(3);
    assertThat(entityDetails.get(0).getEntityRef().getIdentifier()).isEqualTo("terragrunt-configFiles");
    assertThat(entityDetails.get(1).getEntityRef().getIdentifier()).isEqualTo("terragrunt-varFiles");
    assertThat(entityDetails.get(2).getEntityRef().getIdentifier()).isEqualTo("terragrunt-backendFile");
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testValidateResourcesWhenApplyInheritFromPLan() {
    TerragruntApplyStepParameters parameters =
        TerragruntApplyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(TerragruntStepConfigurationParameters.builder()
                               .type(TerragruntStepConfigurationType.INHERIT_FROM_PLAN)
                               .spec(TerragruntExecutionDataParameters.builder().build())
                               .build())
            .build();

    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(parameters).build();
    terragruntApplyStep.validateResources(TerragruntTestStepUtils.getAmbiance(), stepElementParameters);
    verify(pipelineRbacHelper, times(1))
        .checkRuntimePermissions(eq(TerragruntTestStepUtils.getAmbiance()), captor.capture(), eq(true));

    List<EntityDetail> entityDetails = captor.getValue();
    assertThat(entityDetails.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testObtainTaskAfterRbac() {
    TerragruntConfigFilesWrapper configFilesWrapper = TerragruntTestStepUtils.createConfigFilesWrapper();
    LinkedHashMap<String, TerragruntVarFile> varFilesMap = TerragruntTestStepUtils.createVarFilesInline();
    TerragruntBackendConfig backendConfig = TerragruntTestStepUtils.createInlineBackendConfig();

    Map<String, Object> envVars = new HashMap<>() {
      { put("envKey", "envVal"); }
    };

    TerragruntApplyStepParameters parameters =
        TerragruntApplyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(
                TerragruntStepConfigurationParameters.builder()
                    .type(TerragruntStepConfigurationType.INLINE)
                    .spec(TerragruntExecutionDataParameters.builder()
                              .configFiles(configFilesWrapper)
                              .backendConfig(backendConfig)
                              .varFiles(varFilesMap)
                              .terragruntModuleConfig(TerragruntTestStepUtils.createTerragruntModuleConfig())
                              .targets(ParameterField.createValueField(Collections.singletonList("test-target")))
                              .environmentVariables(envVars)
                              .workspace(ParameterField.createValueField("test-workspace"))
                              .build())
                    .build())
            .build();

    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(parameters).build();

    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);

    when(terragruntStepHelper.generateFullIdentifier(any(), any())).thenReturn("testEntityId");
    when(terragruntStepHelper.getGitFetchFilesConfig(any(), any(), any()))
        .thenReturn(TerragruntTestStepUtils.createGitStoreDelegateConfig());
    when(terragruntStepHelper.toStoreDelegateVarFiles(anyMap(), any()))
        .thenReturn(Collections.singletonList(
            InlineStoreDelegateConfig.builder()
                .identifier("test-var1-id")
                .files(Collections.singletonList(InlineFileConfig.builder().content("test-var1-content").build()))
                .build()));
    when(terragruntStepHelper.getBackendConfig(any(), any()))
        .thenReturn(InlineStoreDelegateConfig.builder().identifier("test-backend-id").build());
    when(terragruntStepHelper.getEnvironmentVariablesMap(any())).thenReturn(new HashMap<>());
    when(terragruntStepHelper.getLatestFileId(any())).thenReturn(null);
    Mockito.mockStatic(StepUtils.class);

    terragruntApplyStep.obtainTaskAfterRbac(
        TerragruntTestStepUtils.getAmbiance(), stepElementParameters, StepInputPackage.builder().build());

    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    TerragruntApplyTaskParameters params =
        (TerragruntApplyTaskParameters) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(params.getAccountId()).isEqualTo("test-account");
    assertThat(params.getWorkspace()).isEqualTo("test-workspace");
    assertThat(params.getRunConfiguration().getRunType()).isEqualTo(TerragruntTaskRunType.RUN_MODULE);
    assertThat(params.getRunConfiguration().getPath()).isEqualTo("test-path");
    assertThat(params.getTargets().get(0)).isEqualTo("test-target");
    List<StoreDelegateConfig> inlineVar = params.getVarFiles();
    assertThat(((InlineStoreDelegateConfig) inlineVar.get(0)).getIdentifier()).isEqualTo("test-var1-id");
    assertThat(((InlineStoreDelegateConfig) params.getBackendFilesStore()).getIdentifier())
        .isEqualTo("test-backend-id");
    assertThat(((GitStoreDelegateConfig) params.getConfigFilesStore()).getConnectorName()).isEqualTo("terragrunt");
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testHandleTaskResultWithSecurityContextWhenApplyInline() throws Exception {
    Ambiance ambiance = TerragruntTestStepUtils.getAmbiance();

    List<UnitProgress> unitProgresses = new ArrayList<>();
    unitProgresses.add(UnitProgress.newBuilder().setStatus(UnitStatus.SUCCESS).build());

    TerragruntApplyTaskResponse terragruntTaskNGResponse =
        TerragruntApplyTaskResponse.builder()
            .unitProgressData(UnitProgressData.builder().unitProgresses(unitProgresses).build())
            .outputs("test-output-1")
            .stateFileId("test-stateFileId")
            .build();

    TerragruntApplyStepParameters parameters =
        TerragruntApplyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(
                TerragruntStepConfigurationParameters.builder()
                    .type(TerragruntStepConfigurationType.INLINE)
                    .spec(TerragruntExecutionDataParameters.builder()
                              .configFiles(TerragruntTestStepUtils.createConfigFilesWrapper())
                              .backendConfig(TerragruntTestStepUtils.createInlineBackendConfig())
                              .varFiles(TerragruntTestStepUtils.createVarFilesInline())
                              .terragruntModuleConfig(TerragruntTestStepUtils.createTerragruntModuleConfig())
                              .targets(ParameterField.createValueField(Collections.singletonList("test-target")))
                              .environmentVariables(new HashMap<>())
                              .workspace(ParameterField.createValueField("test-workspace"))
                              .build())

                    .build())
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(parameters).build();

    when(terragruntStepHelper.parseTerragruntOutputs(any())).thenReturn(new HashMap<>() {
      { put("key-output", "value-output"); }
    });

    StepResponse stepResponse = terragruntApplyStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> terragruntTaskNGResponse);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes()).isNotNull();
    assertThat(stepResponse.getStepOutcomes()).hasSize(1);
    StepResponse.StepOutcome stepOutcome = ((List<StepResponse.StepOutcome>) stepResponse.getStepOutcomes()).get(0);
    assertThat(stepOutcome.getOutcome()).isInstanceOf(TerragruntApplyOutcome.class);
    verify(terragruntStepHelper, times(1)).saveRollbackDestroyConfigInline(any(), any(), any());
    verify(terragruntStepHelper, times(1)).updateParentEntityIdAndVersion(any(), any());
    verify(terragruntStepHelper, times(1)).generateFullIdentifier(any(), any());
    verify(terragruntStepHelper, times(1)).parseTerragruntOutputs(any());
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testHandleTaskResultWithSecurityContextWhenApplyInheritFromPlan() throws Exception {
    Ambiance ambiance = TerragruntTestStepUtils.getAmbiance();

    List<UnitProgress> unitProgresses = new ArrayList<>();
    unitProgresses.add(UnitProgress.newBuilder().setStatus(UnitStatus.SUCCESS).build());

    TerragruntApplyTaskResponse terragruntTaskNGResponse =
        TerragruntApplyTaskResponse.builder()
            .unitProgressData(UnitProgressData.builder().unitProgresses(unitProgresses).build())
            .outputs("test-output-1")
            .stateFileId("test-stateFileId")
            .build();

    TerragruntApplyStepParameters parameters =
        TerragruntApplyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(
                TerragruntStepConfigurationParameters.builder()
                    .type(TerragruntStepConfigurationType.INHERIT_FROM_PLAN)
                    .spec(TerragruntExecutionDataParameters.builder()
                              .configFiles(TerragruntTestStepUtils.createConfigFilesWrapper())
                              .backendConfig(TerragruntTestStepUtils.createInlineBackendConfig())
                              .varFiles(TerragruntTestStepUtils.createVarFilesInline())
                              .terragruntModuleConfig(TerragruntTestStepUtils.createTerragruntModuleConfig())
                              .targets(ParameterField.createValueField(Collections.singletonList("test-target")))
                              .environmentVariables(new HashMap<>())
                              .workspace(ParameterField.createValueField("test-workspace"))
                              .build())

                    .build())
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(parameters).build();

    when(terragruntStepHelper.parseTerragruntOutputs(any())).thenReturn(new HashMap<>() {
      { put("key-output", "value-output"); }
    });

    StepResponse stepResponse = terragruntApplyStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> terragruntTaskNGResponse);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes()).isNotNull();
    assertThat(stepResponse.getStepOutcomes()).hasSize(1);
    StepResponse.StepOutcome stepOutcome = ((List<StepResponse.StepOutcome>) stepResponse.getStepOutcomes()).get(0);
    assertThat(stepOutcome.getOutcome()).isInstanceOf(TerragruntApplyOutcome.class);
    verify(terragruntStepHelper, times(1)).saveRollbackDestroyConfigInherited(any(), any());
    verify(terragruntStepHelper, times(1)).updateParentEntityIdAndVersion(any(), any());
    verify(terragruntStepHelper, times(1)).generateFullIdentifier(any(), any());
    verify(terragruntStepHelper, times(1)).parseTerragruntOutputs(any());
  }
}
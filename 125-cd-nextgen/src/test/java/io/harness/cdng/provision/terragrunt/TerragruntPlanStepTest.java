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
import io.harness.cdng.provision.terragrunt.outcome.TerragruntPlanOutcome;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.InlineFileConfig;
import io.harness.delegate.beans.storeconfig.InlineStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.beans.terragrunt.request.TerragruntCommandType;
import io.harness.delegate.beans.terragrunt.request.TerragruntPlanTaskParameters;
import io.harness.delegate.beans.terragrunt.request.TerragruntTaskRunType;
import io.harness.delegate.beans.terragrunt.response.TerragruntPlanTaskResponse;
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
public class TerragruntPlanStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private KryoSerializer kryoSerializer;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock private TerragruntStepHelper terragruntStepHelper;
  @Mock private PipelineRbacHelper pipelineRbacHelper;
  @Mock private StepHelper stepHelper;
  @InjectMocks private TerragruntPlanStep terragruntPlanStep = new TerragruntPlanStep();
  @Captor ArgumentCaptor<List<EntityDetail>> captor;

  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions("accountId", "test-account")
        .putSetupAbstractions("projectIdentifier", "test-project")
        .putSetupAbstractions("orgIdentifier", "test-org")
        .build();
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testValidateResourcesWithGithubStore() {
    TerragruntConfigFilesWrapper configFilesWrapper = TerragruntTestStepUtils.createConfigFilesWrapper();
    LinkedHashMap<String, TerragruntVarFile> varFilesMap = TerragruntTestStepUtils.createVarFilesRemote();
    TerragruntBackendConfig terragruntBackendConfig = TerragruntTestStepUtils.createRemoteBackendConfig();

    TerragruntPlanStepParameters parameters =
        TerragruntPlanStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(TerragruntPlanExecutionDataParameters.builder()
                               .configFiles(configFilesWrapper)
                               .backendConfig(terragruntBackendConfig)
                               .varFiles(varFilesMap)
                               .secretManagerRef(ParameterField.createValueField("test-secretManager"))
                               .build())
            .build();

    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(parameters).build();
    terragruntPlanStep.validateResources(getAmbiance(), stepElementParameters);
    verify(pipelineRbacHelper, times(1)).checkRuntimePermissions(eq(getAmbiance()), captor.capture(), eq(true));

    List<EntityDetail> entityDetails = captor.getValue();
    assertThat(entityDetails.size()).isEqualTo(4);
    assertThat(entityDetails.get(0).getEntityRef().getIdentifier()).isEqualTo("terragrunt-configFiles");
    assertThat(entityDetails.get(1).getEntityRef().getIdentifier()).isEqualTo("terragrunt-varFiles");
    assertThat(entityDetails.get(2).getEntityRef().getIdentifier()).isEqualTo("terragrunt-backendFile");
    assertThat(entityDetails.get(3).getEntityRef().getIdentifier()).isEqualTo("test-secretManager");
  }

  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testobtainTaskAfterRbac() {
    TerragruntConfigFilesWrapper configFilesWrapper = TerragruntTestStepUtils.createConfigFilesWrapper();
    LinkedHashMap<String, TerragruntVarFile> varFilesMap = TerragruntTestStepUtils.createVarFilesInline();
    TerragruntBackendConfig backendConfig = TerragruntTestStepUtils.createInlineBackendConfig();

    Map<String, Object> envVars = new HashMap<>() {
      { put("envKey", "envVal"); }
    };

    TerragruntPlanStepParameters parameters =
        TerragruntPlanStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(TerragruntPlanExecutionDataParameters.builder()
                               .command(TerragruntPlanCommand.APPLY)
                               .configFiles(configFilesWrapper)
                               .backendConfig(backendConfig)
                               .varFiles(varFilesMap)
                               .terragruntModuleConfig(TerragruntTestStepUtils.createTerragruntModuleConfig())
                               .exportTerragruntPlanJson(ParameterField.createValueField(true))
                               .targets(ParameterField.createValueField(Collections.singletonList("test-target")))
                               .environmentVariables(envVars)
                               .workspace(ParameterField.createValueField("test-workspace"))
                               .secretManagerRef(ParameterField.createValueField("test-secretManager"))
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

    terragruntPlanStep.obtainTaskAfterRbac(getAmbiance(), stepElementParameters, StepInputPackage.builder().build());
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    TerragruntPlanTaskParameters params =
        (TerragruntPlanTaskParameters) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(params.getCommandType()).isEqualTo(TerragruntCommandType.APPLY);
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
  public void testhandleTaskResultWithSecurityContext() throws Exception {
    Ambiance ambiance = getAmbiance();

    List<UnitProgress> unitProgresses = new ArrayList<>();
    unitProgresses.add(UnitProgress.newBuilder().setStatus(UnitStatus.SUCCESS).build());

    TerragruntPlanTaskResponse terragruntTaskNGResponse =
        TerragruntPlanTaskResponse.builder()
            .unitProgressData(UnitProgressData.builder().unitProgresses(unitProgresses).build())
            .stateFileId("test-stateFileId")
            .build();

    TerragruntPlanStepParameters parameters =
        TerragruntPlanStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("test-provisionerId"))
            .configuration(TerragruntPlanExecutionDataParameters.builder()
                               .command(TerragruntPlanCommand.APPLY)
                               .configFiles(TerragruntTestStepUtils.createConfigFilesWrapper())
                               .backendConfig(TerragruntTestStepUtils.createInlineBackendConfig())
                               .varFiles(TerragruntTestStepUtils.createVarFilesInline())
                               .terragruntModuleConfig(TerragruntTestStepUtils.createTerragruntModuleConfig())
                               .exportTerragruntPlanJson(ParameterField.createValueField(true))
                               .targets(ParameterField.createValueField(Collections.singletonList("test-target")))
                               .environmentVariables(new HashMap<>())
                               .workspace(ParameterField.createValueField("test-workspace"))
                               .secretManagerRef(ParameterField.createValueField("test-secretManager"))
                               .build())

            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(parameters).build();

    StepResponse stepResponse = terragruntPlanStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> terragruntTaskNGResponse);

    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes()).isNotNull();
    assertThat(stepResponse.getStepOutcomes()).hasSize(1);
    StepResponse.StepOutcome stepOutcome = ((List<StepResponse.StepOutcome>) stepResponse.getStepOutcomes()).get(0);
    assertThat(stepOutcome.getOutcome()).isInstanceOf(TerragruntPlanOutcome.class);
    verify(terragruntStepHelper, times(1)).saveTerragruntInheritOutput(any(), any(), any());
    verify(terragruntStepHelper, times(1)).updateParentEntityIdAndVersion(any(), any());
    verify(terragruntStepHelper, times(1)).generateFullIdentifier(any(), any());
    verify(terragruntStepHelper, times(1)).saveTerragruntPlanExecutionDetails(any(), any(), any(), any());
    verify(terragruntStepHelper, times(1)).saveTerraformPlanJsonOutput(any(), any(), any());
  }
}
/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.terraform;

import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EnvironmentType;
import io.harness.category.element.UnitTests;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.manifest.yaml.GitStoreConfigDTO;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.GithubStoreDTO;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TFTaskType;
import io.harness.delegate.task.terraform.TerraformTaskNGParameters;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ng.core.EntityDetail;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StepUtils.class})
@OwnedBy(HarnessTeam.CDP)
public class TerraformDestroyStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private KryoSerializer kryoSerializer;
  @Mock private TerraformStepHelper terraformStepHelper;
  @Mock private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Mock private PipelineRbacHelper pipelineRbacHelper;
  @Mock private StepHelper stepHelper;
  @Mock private TerraformConfigDAL terraformConfigDAL;
  @InjectMocks private TerraformDestroyStep terraformDestroyStep;
  @Captor ArgumentCaptor<List<EntityDetail>> captor;

  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions("accountId", "test-account")
        .putSetupAbstractions("projectIdentifier", "test-project")
        .putSetupAbstractions("orgIdentifier", "test-org")
        .build();
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testValidateResources() {
    Ambiance ambiance = getAmbiance();
    TerraformConfigFilesWrapper configFilesWrapper = new TerraformConfigFilesWrapper();
    configFilesWrapper.setStore(StoreConfigWrapper.builder()
                                    .spec(GithubStore.builder()
                                              .branch(ParameterField.createValueField("master"))
                                              .gitFetchType(FetchType.BRANCH)
                                              .folderPath(ParameterField.createValueField("Config/"))
                                              .connectorRef(ParameterField.createValueField("terraform"))
                                              .build())
                                    .type(StoreConfigType.GITHUB)
                                    .build());
    RemoteTerraformVarFileSpec remoteTerraformVarFileSpec = new RemoteTerraformVarFileSpec();
    remoteTerraformVarFileSpec.setStore(
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .branch(ParameterField.createValueField("master"))
                      .gitFetchType(FetchType.BRANCH)
                      .paths(ParameterField.createValueField(Collections.singletonList("VarFiles/")))
                      .connectorRef(ParameterField.createValueField("terraform"))
                      .build())
            .type(StoreConfigType.GITHUB)
            .build());
    InlineTerraformVarFileSpec inlineTerraformVarFileSpec = new InlineTerraformVarFileSpec();
    inlineTerraformVarFileSpec.setContent(ParameterField.createValueField("var-content"));
    InlineTerraformBackendConfigSpec inlineTerraformBackendConfigSpec = new InlineTerraformBackendConfigSpec();
    inlineTerraformBackendConfigSpec.setContent(ParameterField.createValueField("back-content"));
    TerraformBackendConfig terraformBackendConfig = new TerraformBackendConfig();
    terraformBackendConfig.setTerraformBackendConfigSpec(inlineTerraformBackendConfigSpec);
    LinkedHashMap<String, TerraformVarFile> varFilesMap = new LinkedHashMap<>();
    varFilesMap.put("var-file-01",
        TerraformVarFile.builder().identifier("var-file-01").type("Inline").spec(inlineTerraformVarFileSpec).build());
    varFilesMap.put("var-file-02",
        TerraformVarFile.builder().identifier("var-file-02").type("Remote").spec(remoteTerraformVarFileSpec).build());
    TerraformDestroyStepParameters destroyStepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("provId_$"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INLINE)
                               .spec(TerraformExecutionDataParameters.builder()
                                         .configFiles(configFilesWrapper)
                                         .varFiles(varFilesMap)
                                         .build())
                               .build())
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(destroyStepParameters).build();
    terraformDestroyStep.validateResources(ambiance, stepElementParameters);
    verify(pipelineRbacHelper, times(1)).checkRuntimePermissions(eq(ambiance), captor.capture(), eq(true));

    List<EntityDetail> entityDetails = captor.getValue();
    assertThat(entityDetails.size()).isEqualTo(2);
    assertThat(entityDetails.get(0).getEntityRef().getIdentifier()).isEqualTo("terraform");
    assertThat(entityDetails.get(0).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
    assertThat(entityDetails.get(1).getEntityRef().getIdentifier()).isEqualTo("terraform");
    assertThat(entityDetails.get(1).getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testobtainTaskAfterRbac() {
    Ambiance ambiance = getAmbiance();
    TerraformConfigFilesWrapper configFilesWrapper = new TerraformConfigFilesWrapper();
    configFilesWrapper.setStore(StoreConfigWrapper.builder()
                                    .spec(GithubStore.builder()
                                              .branch(ParameterField.createValueField("master"))
                                              .gitFetchType(FetchType.BRANCH)
                                              .folderPath(ParameterField.createValueField("Config/"))
                                              .connectorRef(ParameterField.createValueField("terraform"))
                                              .build())
                                    .type(StoreConfigType.GITHUB)
                                    .build());
    RemoteTerraformVarFileSpec remoteTerraformVarFileSpec = new RemoteTerraformVarFileSpec();
    remoteTerraformVarFileSpec.setStore(
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .branch(ParameterField.createValueField("master"))
                      .gitFetchType(FetchType.BRANCH)
                      .paths(ParameterField.createValueField(Collections.singletonList("VarFiles/")))
                      .connectorRef(ParameterField.createValueField("terraform"))
                      .build())
            .type(StoreConfigType.GITHUB)
            .build());
    InlineTerraformVarFileSpec inlineTerraformVarFileSpec = new InlineTerraformVarFileSpec();
    inlineTerraformVarFileSpec.setContent(ParameterField.createValueField("var-content"));
    InlineTerraformBackendConfigSpec inlineTerraformBackendConfigSpec = new InlineTerraformBackendConfigSpec();
    inlineTerraformBackendConfigSpec.setContent(ParameterField.createValueField("back-content"));
    TerraformBackendConfig terraformBackendConfig = new TerraformBackendConfig();
    terraformBackendConfig.setTerraformBackendConfigSpec(inlineTerraformBackendConfigSpec);
    LinkedHashMap<String, TerraformVarFile> varFilesMap = new LinkedHashMap<>();
    varFilesMap.put("var-file-01",
        TerraformVarFile.builder().identifier("var-file-01").type("Inline").spec(inlineTerraformVarFileSpec).build());
    varFilesMap.put("var-file-02",
        TerraformVarFile.builder().identifier("var-file-02").type("Remote").spec(remoteTerraformVarFileSpec).build());
    TerraformDestroyStepParameters destroyStepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("Id"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INLINE)
                               .spec(TerraformExecutionDataParameters.builder()
                                         .configFiles(configFilesWrapper)
                                         .varFiles(varFilesMap)
                                         .build())
                               .build())
            .build();
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitAuthType(GitAuthType.HTTP)
                                    .gitConnectionType(GitConnectionType.ACCOUNT)
                                    .delegateSelectors(Collections.singleton("delegateName"))
                                    .url("https://github.com/wings-software")
                                    .branchName("master")
                                    .build();
    GitStoreDelegateConfig gitStoreDelegateConfig =
        GitStoreDelegateConfig.builder().branch("master").connectorName("terraform").gitConfigDTO(gitConfigDTO).build();
    GitFetchFilesConfig gitFetchFilesConfig = GitFetchFilesConfig.builder()
                                                  .identifier("terraform")
                                                  .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                                  .succeedIfFileNotFound(false)
                                                  .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(destroyStepParameters).build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    doReturn("test-account/test-org/test-project/Id").when(terraformStepHelper).generateFullIdentifier(any(), any());
    doReturn(gitFetchFilesConfig).when(terraformStepHelper).getGitFetchFilesConfig(any(), any(), any());
    doReturn(EnvironmentType.NON_PROD).when(stepHelper).getEnvironmentType(any());
    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    TaskRequest taskRequest =
        terraformDestroyStep.obtainTaskAfterRbac(ambiance, stepElementParameters, stepInputPackage);
    assertThat(taskRequest).isNotNull();
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    TerraformTaskNGParameters taskParameters =
        (TerraformTaskNGParameters) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(taskParameters.getTaskType()).isEqualTo(TFTaskType.DESTROY);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testObtainTaskAfterRbacInheritPlan() {
    Ambiance ambiance = getAmbiance();
    TerraformDestroyStepParameters destroyStepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("Id"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INHERIT_FROM_PLAN)
                               .build())
            .build();
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitAuthType(GitAuthType.HTTP)
                                    .gitConnectionType(GitConnectionType.ACCOUNT)
                                    .delegateSelectors(Collections.singleton("delegateName"))
                                    .url("https://github.com/wings-software")
                                    .branchName("master")
                                    .build();
    GitStoreDelegateConfig gitStoreDelegateConfig =
        GitStoreDelegateConfig.builder().branch("master").connectorName("terraform").gitConfigDTO(gitConfigDTO).build();
    GitFetchFilesConfig gitFetchFilesConfig = GitFetchFilesConfig.builder()
                                                  .identifier("terraform")
                                                  .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                                  .succeedIfFileNotFound(false)
                                                  .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(destroyStepParameters).build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    doReturn("test-account/test-org/test-project/Id").when(terraformStepHelper).generateFullIdentifier(any(), any());
    doReturn(gitFetchFilesConfig).when(terraformStepHelper).getGitFetchFilesConfig(any(), any(), any());
    doReturn(EnvironmentType.NON_PROD).when(stepHelper).getEnvironmentType(any());

    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);

    TerraformInheritOutput inheritOutput =
        TerraformInheritOutput.builder().backendConfig("back-content").workspace("w1").planName("plan").build();
    doReturn(inheritOutput).when(terraformStepHelper).getSavedInheritOutput(any(), any());
    TaskRequest taskRequest =
        terraformDestroyStep.obtainTaskAfterRbac(ambiance, stepElementParameters, stepInputPackage);
    assertThat(taskRequest).isNotNull();
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    TerraformTaskNGParameters taskParameters =
        (TerraformTaskNGParameters) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(taskParameters.getTaskType()).isEqualTo(TFTaskType.DESTROY);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testObtainTaskAfterRbacInheritApply() {
    Ambiance ambiance = getAmbiance();
    TerraformDestroyStepParameters destroyStepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("Id"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INHERIT_FROM_APPLY)
                               .build())
            .build();
    GitConfigDTO gitConfigDTO = GitConfigDTO.builder()
                                    .gitAuthType(GitAuthType.HTTP)
                                    .gitConnectionType(GitConnectionType.ACCOUNT)
                                    .delegateSelectors(Collections.singleton("delegateName"))
                                    .url("https://github.com/wings-software")
                                    .branchName("master")
                                    .build();
    GitStoreDelegateConfig gitStoreDelegateConfig =
        GitStoreDelegateConfig.builder().branch("master").connectorName("terraform").gitConfigDTO(gitConfigDTO).build();
    GitFetchFilesConfig gitFetchFilesConfig = GitFetchFilesConfig.builder()
                                                  .identifier("terraform")
                                                  .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                                  .succeedIfFileNotFound(false)
                                                  .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(destroyStepParameters).build();
    StepInputPackage stepInputPackage = StepInputPackage.builder().build();
    doReturn("test-account/test-org/test-project/Id").when(terraformStepHelper).generateFullIdentifier(any(), any());
    doReturn(gitFetchFilesConfig).when(terraformStepHelper).getGitFetchFilesConfig(any(), any(), any());
    doReturn(EnvironmentType.NON_PROD).when(stepHelper).getEnvironmentType(any());

    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    GitStoreConfigDTO configFiles = GithubStoreDTO.builder().build();
    TerraformConfig terraformConfig =
        TerraformConfig.builder().backendConfig("back-content").workspace("w1").configFiles(configFiles).build();

    doReturn(terraformConfig).when(terraformStepHelper).getLastSuccessfulApplyConfig(any(), any());
    TaskRequest taskRequest =
        terraformDestroyStep.obtainTaskAfterRbac(ambiance, stepElementParameters, stepInputPackage);
    assertThat(taskRequest).isNotNull();
    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    TerraformTaskNGParameters taskParameters =
        (TerraformTaskNGParameters) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(taskParameters.getTaskType()).isEqualTo(TFTaskType.DESTROY);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testHandleTaskResultWithSecurityContext() throws Exception {
    Ambiance ambiance = getAmbiance();
    TerraformDestroyStepParameters destroyStepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("Id"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INHERIT_FROM_PLAN)
                               .build())
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(destroyStepParameters).build();
    doReturn("test-account/test-org/test-project/Id").when(terraformStepHelper).generateFullIdentifier(any(), any());
    List<UnitProgress> unitProgresses = Collections.singletonList(UnitProgress.newBuilder().build());
    UnitProgressData unitProgressData = UnitProgressData.builder().unitProgresses(unitProgresses).build();
    TerraformTaskNGResponse terraformTaskNGResponse = TerraformTaskNGResponse.builder()
                                                          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                          .unitProgressData(unitProgressData)
                                                          .build();
    StepResponse stepResponse = terraformDestroyStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> terraformTaskNGResponse);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getStepOutcomes()).isNotNull();
    verify(terraformConfigDAL, times(1)).clearTerraformConfig(any(), any());
    verify(terraformStepHelper, times(1)).updateParentEntityIdAndVersion(any(), any());
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetStepParametersClass() {
    assertThat(terraformDestroyStep.getStepParametersClass()).isEqualTo(StepElementParameters.class);
  }

  @Test // Different Status
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void handleTaskResultWithSecurityContextDifferentStatus() throws Exception {
    Ambiance ambiance = getAmbiance();
    TerraformDestroyStepParameters destroyStepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("Id"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INHERIT_FROM_PLAN)
                               .build())
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(destroyStepParameters).build();
    doReturn("test-account/test-org/test-project/Id").when(terraformStepHelper).generateFullIdentifier(any(), any());
    List<UnitProgress> unitProgresses = Collections.singletonList(UnitProgress.newBuilder().build());
    UnitProgressData unitProgressData = UnitProgressData.builder().unitProgresses(unitProgresses).build();
    TerraformTaskNGResponse terraformTaskNGResponseFailure = TerraformTaskNGResponse.builder()
                                                                 .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                                                 .unitProgressData(unitProgressData)
                                                                 .build();
    StepResponse stepResponse = terraformDestroyStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> terraformTaskNGResponseFailure);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
    assertThat(stepResponse.getStepOutcomes()).isNotNull();

    TerraformTaskNGResponse terraformTaskNGResponseRunning = TerraformTaskNGResponse.builder()
                                                                 .commandExecutionStatus(CommandExecutionStatus.RUNNING)
                                                                 .unitProgressData(unitProgressData)
                                                                 .build();
    stepResponse = terraformDestroyStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> terraformTaskNGResponseRunning);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.RUNNING);
    assertThat(stepResponse.getStepOutcomes()).isNotNull();

    TerraformTaskNGResponse terraformTaskNGResponseQueued = TerraformTaskNGResponse.builder()
                                                                .commandExecutionStatus(CommandExecutionStatus.QUEUED)
                                                                .unitProgressData(unitProgressData)
                                                                .build();
    stepResponse = terraformDestroyStep.handleTaskResultWithSecurityContext(
        ambiance, stepElementParameters, () -> terraformTaskNGResponseQueued);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.QUEUED);
    assertThat(stepResponse.getStepOutcomes()).isNotNull();
    String message =
        String.format("Unhandled type CommandExecutionStatus: " + CommandExecutionStatus.SKIPPED, WingsException.USER);
    try {
      TerraformTaskNGResponse terraformTaskNGResponseSkipped =
          TerraformTaskNGResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.SKIPPED)
              .unitProgressData(unitProgressData)
              .build();
      terraformDestroyStep.handleTaskResultWithSecurityContext(
          ambiance, stepElementParameters, () -> terraformTaskNGResponseSkipped);
    } catch (InvalidRequestException invalidRequestException) {
      assertThat(invalidRequestException.getMessage()).isEqualTo(message);
    }
  }
}

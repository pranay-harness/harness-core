/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.terraform;

import static io.harness.delegate.beans.connector.ConnectorType.GITHUB;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SATYAM;
import static io.harness.rule.OwnerRule.TMACARI;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.fileservice.FileServiceClientFactory;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.yaml.BitBucketStoreDTO;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitLabStoreDTO;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.GitStoreConfigDTO;
import io.harness.cdng.manifest.yaml.GitStoreDTO;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.GithubStoreDTO;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.common.ParameterFieldHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.task.terraform.InlineTerraformVarFileInfo;
import io.harness.delegate.task.terraform.RemoteTerraformVarFileInfo;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.delegate.task.terraform.TerraformVarFileInfo;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.filesystem.FileIo;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.remote.client.RestClientUtils;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptionType;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RestClientUtils.class})
@OwnedBy(HarnessTeam.CDP)
public class TerraformStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private HPersistence mockPersistence;
  @Mock private K8sStepHelper mockK8sStepHelper;
  @Mock private ExecutionSweepingOutputService mockExecutionSweepingOutputService;
  @Mock private GitConfigAuthenticationInfoHelper mockGitConfigAuthenticationInfoHelper;
  @Mock private FileServiceClientFactory mockFileService;
  @Mock private SecretManagerClientService mockSecretManagerClientService;
  @Mock private TerraformConfigDAL terraformConfigDAL;
  @InjectMocks private TerraformStepHelper helper;

  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions("accountId", "test-account")
        .putSetupAbstractions("projectIdentifier", "test-project")
        .putSetupAbstractions("orgIdentifier", "test-org")
        .setPlanExecutionId("exec_id")
        .build();
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testSaveTerraformInheritOutput() {
    Ambiance ambiance = getAmbiance();
    TerraformConfigFilesWrapper configFilesWrapper = new TerraformConfigFilesWrapper();
    configFilesWrapper.setStore(StoreConfigWrapper.builder()
                                    .spec(GithubStore.builder()
                                              .branch(ParameterField.createValueField("master"))
                                              .gitFetchType(FetchType.BRANCH)
                                              .folderPath(ParameterField.createValueField("Config/"))
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
    varFilesMap.put("var-file-1",
        TerraformVarFile.builder().identifier("var-file-1").type("Inline").spec(inlineTerraformVarFileSpec).build());
    TerraformPlanStepParameters planStepParameters =
        TerraformPlanStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("provId"))
            .configuration(TerraformPlanExecutionDataParameters.builder()
                               .configFiles(configFilesWrapper)
                               .command(TerraformPlanCommand.APPLY)
                               .secretManagerRef(ParameterField.createValueField("secret"))
                               .varFiles(varFilesMap)
                               .environmentVariables(ImmutableMap.of("KEY", ParameterField.createValueField("VAL")))
                               .backendConfig(terraformBackendConfig)
                               .build())
            .build();
    TerraformTaskNGResponse response =
        TerraformTaskNGResponse.builder()
            .commitIdForConfigFilesMap(ImmutableMap.of(TerraformStepHelper.TF_CONFIG_FILES, "commit-1"))
            .build();
    doReturn(LocalConfigDTO.builder().encryptionType(EncryptionType.LOCAL).build())
        .when(mockSecretManagerClientService)
        .getSecretManager(anyString(), anyString(), anyString(), anyString(), anyBoolean());
    helper.saveTerraformInheritOutput(planStepParameters, response, ambiance);
    ArgumentCaptor<TerraformInheritOutput> captor = ArgumentCaptor.forClass(TerraformInheritOutput.class);
    verify(mockExecutionSweepingOutputService).consume(any(), anyString(), captor.capture(), anyString());
    TerraformInheritOutput output = captor.getValue();
    assertThat(output).isNotNull();
    GitStoreConfig configFiles = output.getConfigFiles();
    assertThat(configFiles).isNotNull();
    assertThat(configFiles.getGitFetchType()).isEqualTo(FetchType.COMMIT);
    String commitId = ParameterFieldHelper.getParameterFieldValue(configFiles.getCommitId());
    assertThat(commitId).isEqualTo("commit-1");
    List<TerraformVarFileConfig> varFileConfigs = output.getVarFileConfigs();
    assertThat(varFileConfigs).isNotNull();
    assertThat(varFileConfigs.size()).isEqualTo(1);
    assertThat(varFileConfigs.get(0) instanceof TerraformInlineVarFileConfig).isTrue();
    assertThat(((TerraformInlineVarFileConfig) varFileConfigs.get(0)).getVarFileContent()).isEqualTo("var-content");
    assertThat(output.getBackendConfig()).isEqualTo("back-content");
    assertThat(output.getEnvironmentVariables()).isNotNull();
    assertThat(output.getEnvironmentVariables().size()).isEqualTo(1);
    assertThat(output.getEnvironmentVariables().get("KEY")).isEqualTo("VAL");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testSaveRollbackDestroyConfigInline() {
    Ambiance ambiance = getAmbiance();
    TerraformConfigFilesWrapper configFilesWrapper = new TerraformConfigFilesWrapper();
    configFilesWrapper.setStore(StoreConfigWrapper.builder()
                                    .spec(GithubStore.builder()
                                              .branch(ParameterField.createValueField("master"))
                                              .gitFetchType(FetchType.BRANCH)
                                              .folderPath(ParameterField.createValueField("Config/"))
                                              .build())
                                    .type(StoreConfigType.GITHUB)
                                    .build());
    RemoteTerraformVarFileSpec remoteTerraformVarFileSpec = new RemoteTerraformVarFileSpec();
    remoteTerraformVarFileSpec.setStore(StoreConfigWrapper.builder()
                                            .spec(GitLabStore.builder()
                                                      .branch(ParameterField.createValueField("master"))
                                                      .gitFetchType(FetchType.BRANCH)
                                                      .folderPath(ParameterField.createValueField("VarFiles/"))
                                                      .build())
                                            .type(StoreConfigType.GITLAB)
                                            .build());
    LinkedHashMap<String, TerraformVarFile> varFilesMap = new LinkedHashMap<>();
    varFilesMap.put("var-file-1",
        TerraformVarFile.builder().identifier("var-file-1").type("Inline").spec(remoteTerraformVarFileSpec).build());
    TerraformApplyStepParameters parameters = TerraformApplyStepParameters.infoBuilder()
                                                  .provisionerIdentifier(ParameterField.createValueField("provId_$"))
                                                  .configuration(TerraformStepConfigurationParameters.builder()
                                                                     .type(TerraformStepConfigurationType.INLINE)
                                                                     .spec(TerraformExecutionDataParameters.builder()
                                                                               .configFiles(configFilesWrapper)
                                                                               .varFiles(varFilesMap)
                                                                               .build())
                                                                     .build())
                                                  .build();
    TerraformTaskNGResponse response =
        TerraformTaskNGResponse.builder()
            .commitIdForConfigFilesMap(ImmutableMap.of(TerraformStepHelper.TF_CONFIG_FILES, "commit-1",
                String.format(TerraformStepHelper.TF_VAR_FILES, 1), "commit-2"))
            .build();
    helper.saveRollbackDestroyConfigInline(parameters, response, ambiance);
    ArgumentCaptor<TerraformConfig> captor = ArgumentCaptor.forClass(TerraformConfig.class);
    verify(terraformConfigDAL).saveTerraformConfig(captor.capture());
    TerraformConfig config = captor.getValue();
    assertThat(config).isNotNull();
    assertThat(config.getAccountId()).isEqualTo("test-account");
    assertThat(config.getOrgId()).isEqualTo("test-org");
    assertThat(config.getProjectId()).isEqualTo("test-project");
    GitStoreConfigDTO configFiles = config.getConfigFiles();
    assertThat(configFiles instanceof GithubStoreDTO).isTrue();
    GithubStoreDTO githubStoreDTO = (GithubStoreDTO) configFiles;
    assertThat(githubStoreDTO.getGitFetchType()).isEqualTo(FetchType.COMMIT);
    assertThat(githubStoreDTO.getCommitId()).isEqualTo("commit-1");
    List<TerraformVarFileConfig> varFileConfigs = config.getVarFileConfigs();
    assertThat(varFileConfigs).isNotNull();
    assertThat(varFileConfigs.size()).isEqualTo(1);
    TerraformVarFileConfig terraformVarFileConfig = varFileConfigs.get(0);
    assertThat(terraformVarFileConfig instanceof TerraformRemoteVarFileConfig).isTrue();
    TerraformRemoteVarFileConfig remoteVarFileConfig = (TerraformRemoteVarFileConfig) terraformVarFileConfig;
    GitStoreConfigDTO gitStoreConfigDTO = remoteVarFileConfig.getGitStoreConfigDTO();
    assertThat(gitStoreConfigDTO instanceof GitLabStoreDTO).isTrue();
    GitLabStoreDTO gitLabStoreDTO = (GitLabStoreDTO) gitStoreConfigDTO;
    assertThat(gitLabStoreDTO.getGitFetchType()).isEqualTo(FetchType.COMMIT);
    assertThat(gitLabStoreDTO.getCommitId()).isEqualTo("commit-2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testToTerraformVarFileInfo() {
    Ambiance ambiance = getAmbiance();
    RemoteTerraformVarFileSpec remoteTerraformVarFileSpec = new RemoteTerraformVarFileSpec();
    remoteTerraformVarFileSpec.setStore(
        StoreConfigWrapper.builder()
            .spec(GitLabStore.builder()
                      .branch(ParameterField.createValueField("master"))
                      .gitFetchType(FetchType.BRANCH)
                      .paths(ParameterField.createValueField(Collections.singletonList("VarFiles/")))
                      .connectorRef(ParameterField.createValueField("ConnectorRef"))
                      .build())
            .type(StoreConfigType.GITLAB)
            .build());
    InlineTerraformVarFileSpec inlineTerraformVarFileSpec = new InlineTerraformVarFileSpec();
    inlineTerraformVarFileSpec.setContent(ParameterField.createValueField("var-content"));
    Map<String, TerraformVarFile> varFilesMap = ImmutableMap.of("var-file-00",
        TerraformVarFile.builder().identifier("var-file-00").type("Inline").spec(inlineTerraformVarFileSpec).build(),
        "var-file-01",
        TerraformVarFile.builder().identifier("var-file-01").type("Remote").spec(remoteTerraformVarFileSpec).build());
    doReturn(
        ConnectorInfoDTO.builder().connectorConfig(GitConfigDTO.builder().gitAuthType(GitAuthType.SSH).build()).build())
        .when(mockK8sStepHelper)
        .getConnector(anyString(), any());
    doReturn(SSHKeySpecDTO.builder().build())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getSSHKey(any(), anyString(), anyString(), anyString());
    doReturn(Collections.emptyList())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getEncryptedDataDetails(any(), any(), any());
    List<TerraformVarFileInfo> terraformVarFileInfos = helper.toTerraformVarFileInfo(varFilesMap, ambiance);
    assertThat(terraformVarFileInfos).isNotNull();
    assertThat(terraformVarFileInfos.size()).isEqualTo(2);
    TerraformVarFileInfo terraformVarFileInfo = terraformVarFileInfos.get(0);
    assertThat(terraformVarFileInfo instanceof InlineTerraformVarFileInfo).isTrue();
    InlineTerraformVarFileInfo inlineTerraformVarFileInfo = (InlineTerraformVarFileInfo) terraformVarFileInfo;
    assertThat(inlineTerraformVarFileInfo.getVarFileContent()).isEqualTo("var-content");
    terraformVarFileInfo = terraformVarFileInfos.get(1);
    assertThat(terraformVarFileInfo instanceof RemoteTerraformVarFileInfo).isTrue();
    RemoteTerraformVarFileInfo remoteTerraformVarFileInfo = (RemoteTerraformVarFileInfo) terraformVarFileInfo;
    assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getBranch())
        .isEqualTo("master");
    assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getPaths().get(0))
        .isEqualTo("VarFiles/");
    assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getFetchType())
        .isEqualTo(FetchType.BRANCH);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testGenerateFullIdentifier() throws IOException {
    String entityId = helper.generateFullIdentifier("tfplan_$", getAmbiance());
    FileIo.createDirectoryIfDoesNotExist(entityId);
    assertThat(FileIo.checkIfFileExist(entityId)).isTrue();
    FileUtils.deleteQuietly(new File(entityId));
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testGenerateFullIdentifierInvalidProvisionerIdentifer() {
    try {
      helper.generateFullIdentifier("tfplan_ $", getAmbiance());
    } catch (InvalidRequestException invalidRequestException) {
      assertThat(invalidRequestException.getMessage())
          .isEqualTo("Provisioner Identifier cannot contain special characters or spaces: [tfplan_ $]");
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testPrepareTerraformVarFileInfo() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder()
                                         .name("terraform")
                                         .identifier("terraform")
                                         .connectorType(GITHUB)
                                         .connectorConfig(GitConfigDTO.builder()
                                                              .gitAuthType(GitAuthType.HTTP)
                                                              .gitConnectionType(GitConnectionType.ACCOUNT)
                                                              .delegateSelectors(Collections.singleton("delegateName"))
                                                              .url("https://github.com/wings-software")
                                                              .branchName("master")
                                                              .build())
                                         .build();

    doReturn(connectorInfo).when(mockK8sStepHelper).getConnector(anyString(), any());
    doReturn(SSHKeySpecDTO.builder().build())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getSSHKey(any(), anyString(), anyString(), anyString());
    doReturn(Collections.emptyList())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getEncryptedDataDetails(any(), any(), any());

    List<TerraformVarFileConfig> varFileConfigs = new LinkedList<>();

    GitStoreConfigDTO configFiles1 = GithubStoreDTO.builder()
                                         .branch("master")
                                         .repoName("terraform")
                                         .paths(Collections.singletonList("VarFiles/"))
                                         .connectorRef("terraform")
                                         .gitFetchType(FetchType.COMMIT)
                                         .commitId("commit")
                                         .build();

    TerraformVarFileConfig inlineFileConfig =
        TerraformInlineVarFileConfig.builder().varFileContent("var-content").build();
    TerraformVarFileConfig remoteFileConfig =
        TerraformRemoteVarFileConfig.builder().gitStoreConfigDTO(configFiles1).build();
    varFileConfigs.add(inlineFileConfig);
    varFileConfigs.add(remoteFileConfig);

    List<TerraformVarFileInfo> terraformVarFileInfos = helper.prepareTerraformVarFileInfo(varFileConfigs, ambiance);
    assertThat(terraformVarFileInfos.size()).isEqualTo(2);
    for (TerraformVarFileInfo terraformVarFileInfo : terraformVarFileInfos) {
      if (terraformVarFileInfo instanceof InlineTerraformVarFileInfo) {
        InlineTerraformVarFileInfo inlineTerraformVarFileInfo = (InlineTerraformVarFileInfo) terraformVarFileInfo;
        assertThat(inlineTerraformVarFileInfo.getVarFileContent()).isEqualTo("var-content");
      } else if (terraformVarFileInfo instanceof RemoteTerraformVarFileInfo) {
        RemoteTerraformVarFileInfo remoteTerraformVarFileInfo = (RemoteTerraformVarFileInfo) terraformVarFileInfo;
        assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getIdentifier()).isEqualTo("TF_VAR_FILES_1");
        assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getBranch())
            .isEqualTo("master");
        assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getPaths().size())
            .isEqualTo(1);
        assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getPaths().get(0))
            .isEqualTo("VarFiles/");
        assertThat(remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getConnectorName())
            .isEqualTo("terraform");
        assertThat(
            remoteTerraformVarFileInfo.getGitFetchFilesConfig().getGitStoreDelegateConfig().getGitConfigDTO().getUrl())
            .isEqualTo("https://github.com/wings-software/terraform");
      }
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testPrepareTerraformVarFileInfoEmpty() {
    Ambiance ambiance = getAmbiance();
    List<TerraformVarFileConfig> varFileConfigs = new LinkedList<>();
    List<TerraformVarFileInfo> terraformVarFileInfos = helper.prepareTerraformVarFileInfo(varFileConfigs, ambiance);
    assertThat(terraformVarFileInfos).isEmpty();
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testToTerraformVarFileConfigEmpty() {
    Ambiance ambiance = getAmbiance();
    Map<String, TerraformVarFile> varFilesMap = new HashMap<>();
    TerraformTaskNGResponse terraformTaskNGResponse = TerraformTaskNGResponse.builder().build();
    List<TerraformVarFileConfig> terraformVarFileConfig =
        helper.toTerraformVarFileConfig(varFilesMap, terraformTaskNGResponse, ambiance);
    assertThat(terraformVarFileConfig).isEmpty();
  }

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testGetTerraformPlanName() {
    Ambiance ambiance = getAmbiance();
    String planName = helper.getTerraformPlanName(TerraformPlanCommand.APPLY, ambiance);
    assertThat(planName).isEqualTo("tfPlan-exec-id");
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testToTerraformVarFileInfoEmpty() {
    Ambiance ambiance = getAmbiance();
    Map<String, TerraformVarFile> varFilesMap = new HashMap<>();
    List<TerraformVarFileInfo> terraformVarFileInfo = helper.toTerraformVarFileInfo(varFilesMap, ambiance);
    assertThat(terraformVarFileInfo).isEmpty();
  }

  @Test(expected = NullPointerException.class)
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testPrepareTerraformVarFileInfoNegativeScenario() {
    Ambiance ambiance = getAmbiance();
    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder()
                                         .name("terraform")
                                         .identifier("terraform")
                                         .connectorType(GITHUB)
                                         .connectorConfig(GitConfigDTO.builder()
                                                              .gitAuthType(GitAuthType.HTTP)
                                                              .gitConnectionType(GitConnectionType.ACCOUNT)
                                                              .delegateSelectors(Collections.singleton("delegateName"))
                                                              .url("https://github.com/wings-software")
                                                              .branchName("master")
                                                              .build())
                                         .build();

    doReturn(connectorInfo).when(mockK8sStepHelper).getConnector(anyString(), any());
    doReturn(SSHKeySpecDTO.builder().build())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getSSHKey(any(), anyString(), anyString(), anyString());
    doReturn(Collections.emptyList())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getEncryptedDataDetails(any(), any(), any());

    List<TerraformVarFileConfig> varFileConfigs = new LinkedList<>();
    TerraformVarFileConfig remoteFileConfig = TerraformRemoteVarFileConfig.builder().build();
    varFileConfigs.add(remoteFileConfig);

    helper.prepareTerraformVarFileInfo(varFileConfigs, ambiance);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testPrepareEntityDetailsForVarFiles() {
    RemoteTerraformVarFileSpec remoteTerraformVarFileSpec = new RemoteTerraformVarFileSpec();
    remoteTerraformVarFileSpec.setStore(
        StoreConfigWrapper.builder()
            .spec(GitLabStore.builder()
                      .branch(ParameterField.createValueField("master"))
                      .gitFetchType(FetchType.BRANCH)
                      .paths(ParameterField.createValueField(Collections.singletonList("VarFiles/")))
                      .connectorRef(ParameterField.createValueField("ConnectorRef"))
                      .build())
            .type(StoreConfigType.GITLAB)
            .build());
    InlineTerraformVarFileSpec inlineTerraformVarFileSpec = new InlineTerraformVarFileSpec();
    inlineTerraformVarFileSpec.setContent(ParameterField.createValueField("var-content"));
    Map<String, TerraformVarFile> varFilesMap = ImmutableMap.of("var-file-00",
        TerraformVarFile.builder().identifier("var-file-00").type("Inline").spec(inlineTerraformVarFileSpec).build(),
        "var-file-01",
        TerraformVarFile.builder().identifier("var-file-01").type("Remote").spec(remoteTerraformVarFileSpec).build());
    List<EntityDetail> entityDetails =
        helper.prepareEntityDetailsForVarFiles("test-account", "test-org", "test-project", varFilesMap);
    // NULL CASES
    EntityDetail entityDetail = entityDetails.get(0);
    assertThat(entityDetail.getEntityRef().getIdentifier()).isEqualTo("ConnectorRef");
    assertThat(entityDetail.getEntityRef().getOrgIdentifier()).isEqualTo("test-org");
    assertThat(entityDetail.getEntityRef().getProjectIdentifier()).isEqualTo("test-project");
    assertThat(entityDetail.getEntityRef().getAccountIdentifier()).isEqualTo("test-account");
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class) // incomplete
  public void testSaveRollbackDestroyConfigInherited() {
    Ambiance ambiance = getAmbiance();
    doReturn(
        ConnectorInfoDTO.builder().connectorConfig(GitConfigDTO.builder().gitAuthType(GitAuthType.SSH).build()).build())
        .when(mockK8sStepHelper)
        .getConnector(anyString(), any());
    doReturn(SSHKeySpecDTO.builder().build())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getSSHKey(any(), anyString(), anyString(), anyString());
    doReturn(Collections.emptyList())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getEncryptedDataDetails(any(), any(), any());

    TerraformApplyStepParameters parameters =
        TerraformApplyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("provId_$"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INLINE)
                               .spec(TerraformExecutionDataParameters.builder().build())
                               .build())
            .build();

    GitStoreConfigDTO configFiles = GithubStoreDTO.builder().branch("master").connectorRef("terraform").build();
    TerraformConfig terraformConfig =
        TerraformConfig.builder().backendConfig("back-content").workspace("w1").configFiles(configFiles).build();
    ExecutionSweepingOutput terraformInheritOutput = TerraformInheritOutput.builder()
                                                         .configFiles(terraformConfig.configFiles.toGitStoreConfig())
                                                         .varFileConfigs(terraformConfig.getVarFileConfigs())
                                                         .backendConfig(terraformConfig.backendConfig)
                                                         .environmentVariables(terraformConfig.environmentVariables)
                                                         .targets(terraformConfig.targets)
                                                         .workspace(terraformConfig.workspace)
                                                         .build();
    String inheritOutputName = "tfInheritOutput_test-account/test-org/test-project/provId_$";
    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(terraformInheritOutput).build();
    Mockito.doReturn(optionalSweepingOutput)
        .when(mockExecutionSweepingOutputService)
        .resolveOptional(ambiance, RefObjectUtils.getSweepingOutputRefObject(inheritOutputName));
    helper.saveRollbackDestroyConfigInherited(parameters, ambiance);
    ArgumentCaptor<TerraformConfig> captor = ArgumentCaptor.forClass(TerraformConfig.class);
    then(terraformConfigDAL).should(times(1)).saveTerraformConfig(captor.capture());
    TerraformConfig config = captor.getValue();
    assertThat(config).isNotNull();
    assertThat(config.getAccountId()).isEqualTo("test-account");
    assertThat(config.getOrgId()).isEqualTo("test-org");
    assertThat(config.getProjectId()).isEqualTo("test-project");
  }

  @Test(expected = NullPointerException.class)
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testSaveRollbackDestroyConfigInheritedNegativeScenario() {
    Ambiance ambiance = getAmbiance();
    doReturn(
        ConnectorInfoDTO.builder().connectorConfig(GitConfigDTO.builder().gitAuthType(GitAuthType.SSH).build()).build())
        .when(mockK8sStepHelper)
        .getConnector(anyString(), any());
    doReturn(SSHKeySpecDTO.builder().build())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getSSHKey(any(), anyString(), anyString(), anyString());
    doReturn(Collections.emptyList())
        .when(mockGitConfigAuthenticationInfoHelper)
        .getEncryptedDataDetails(any(), any(), any());

    TerraformApplyStepParameters parameters =
        TerraformApplyStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("provId_$"))
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INLINE)
                               .spec(TerraformExecutionDataParameters.builder().build())
                               .build())
            .build();
    TerraformConfig terraformConfig = TerraformConfig.builder().backendConfig("back-content").workspace("w1").build();
    ExecutionSweepingOutput terraformInheritOutput = TerraformInheritOutput.builder()
                                                         .varFileConfigs(terraformConfig.getVarFileConfigs())
                                                         .backendConfig(terraformConfig.backendConfig)
                                                         .environmentVariables(terraformConfig.environmentVariables)
                                                         .targets(terraformConfig.targets)
                                                         .workspace(terraformConfig.workspace)
                                                         .build();
    String inheritOutputName = "tfInheritOutput_test-account/test-org/test-project/provId_$";
    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().found(true).output(terraformInheritOutput).build();
    Mockito.doReturn(optionalSweepingOutput)
        .when(mockExecutionSweepingOutputService)
        .resolveOptional(ambiance, RefObjectUtils.getSweepingOutputRefObject(inheritOutputName));
    helper.saveRollbackDestroyConfigInherited(parameters, ambiance);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testSaveTerraformConfig() {
    Ambiance ambiance = getAmbiance();
    ArgumentCaptor<TerraformConfig> captor = ArgumentCaptor.forClass(TerraformConfig.class);
    GitStoreConfigDTO configFiles = GithubStoreDTO.builder().branch("master").connectorRef("terraform").build();
    TerraformConfig terraformConfig =
        TerraformConfig.builder().backendConfig("back-content").workspace("w1").configFiles(configFiles).build();
    helper.saveTerraformConfig(terraformConfig, ambiance);
    then(terraformConfigDAL).should(times(1)).saveTerraformConfig(captor.capture());
    System.out.println("");
    TerraformConfig config = captor.getValue();
    assertThat(config.getVarFileConfigs()).isNull();
    assertThat(config.getAccountId()).isEqualTo("test-account");
    assertThat(config.getConfigFiles().toGitStoreConfig().getConnectorRef().getValue()).isEqualTo("terraform");
    assertThat(config.getConfigFiles().toGitStoreConfig().getBranch().getValue()).isEqualTo("master");
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetLatestFileIdNegativeScenario() {
    String entityId = "test-account/test-org/test-project/provId_$";
    String str = String.format("Unable to call fileservice to fetch latest file id for entityId: [%s]", entityId);
    try {
      helper.getLatestFileId(entityId);
    } catch (InvalidRequestException invalidRequestException) {
      assertThat(invalidRequestException.getMessage()).isEqualTo(str);
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetLatestFileId() {
    String entityId = "test-account/test-org/test-project/provId_$";
    String str = String.format("Unable to call fileservice to fetch latest file id for entityId: [%s]", entityId);
    mockStatic(RestClientUtils.class);
    try {
      helper.getLatestFileId(entityId);
      PowerMockito.verifyStatic(RestClientUtils.class, times(1));
    } catch (InvalidRequestException invalidRequestException) {
      assertThat(invalidRequestException.getMessage()).isEqualTo(str);
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testUpdateParentEntityIdAndVersionNegativeScenario() {
    String entityId = "test-account/test-org/test-project/provId_$";
    String stateFileId = "";
    String str =
        format("Unable to update StateFile version for entityId: [%s], Please try re-running pipeline", entityId);
    assertThatThrownBy(() -> helper.updateParentEntityIdAndVersion(entityId, stateFileId)).hasMessageContaining(str);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testUpdateParentEntityIdAndVersion() {
    String entityId = "test-account/test-org/test-project/provId_$";
    String stateFileId = "";
    mockStatic(RestClientUtils.class);
    String str =
        format("Unable to update StateFile version for entityId: [%s], Please try re-running pipeline", entityId);
    try {
      helper.updateParentEntityIdAndVersion(entityId, stateFileId);
      PowerMockito.verifyStatic(RestClientUtils.class, times(1));
    } catch (InvalidRequestException invalidRequestException) {
      assertThat(invalidRequestException.getMessage()).isEqualTo(str);
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testValidateApplyStepParamsInline() {
    TerraformApplyStepParameters stepParameters =
        TerraformApplyStepParameters.infoBuilder()
            .configuration(
                TerraformStepConfigurationParameters.builder().type(TerraformStepConfigurationType.INLINE).build())
            .build();
    helper.validateApplyStepParamsInline(stepParameters);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testValidateApplyStepParamsInlineNegativeScenario() {
    TerraformApplyStepParameters stepParameters =
        TerraformApplyStepParameters.infoBuilder()
            .configuration(
                TerraformStepConfigurationParameters.builder().type(TerraformStepConfigurationType.INLINE).build())
            .build();
    try {
      helper.validateApplyStepParamsInline(stepParameters);
    } catch (GeneralException generalException) {
      assertThat(generalException.getMessage()).isEqualTo("Apply Step configuration is NULL");
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testValidateDestroyStepParamsInline() {
    TerraformDestroyStepParameters stepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .configuration(
                TerraformStepConfigurationParameters.builder().type(TerraformStepConfigurationType.INLINE).build())
            .build();
    helper.validateDestroyStepParamsInline(stepParameters);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testValidateDestroyStepParamsInlineNegativeScenario() {
    TerraformDestroyStepParameters stepParameters = new TerraformDestroyStepParameters();
    try {
      helper.validateDestroyStepParamsInline(stepParameters);
    } catch (GeneralException generalException) {
      assertThat(generalException.getMessage()).isEqualTo("Destroy Step configuration is NULL");
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testValidateDestroyStepConfigFilesInline() {
    TerraformConfigFilesWrapper configFilesWrapper = new TerraformConfigFilesWrapper();
    configFilesWrapper.setStore(StoreConfigWrapper.builder()
                                    .spec(GithubStore.builder()
                                              .branch(ParameterField.createValueField("master"))
                                              .gitFetchType(FetchType.BRANCH)
                                              .connectorRef(ParameterField.createValueField("terraform"))
                                              .folderPath(ParameterField.createValueField("Config/"))
                                              .build())
                                    .type(StoreConfigType.GITHUB)
                                    .build());
    TerraformDestroyStepParameters stepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INLINE)
                               .spec(TerraformExecutionDataParameters.builder().configFiles(configFilesWrapper).build())
                               .build())
            .build();
    helper.validateDestroyStepConfigFilesInline(stepParameters);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testValidateDestroyStepConfigFilesInlineNegativeScenario() {
    TerraformConfigFilesWrapper configFilesWrapper = new TerraformConfigFilesWrapper();
    TerraformDestroyStepParameters stepParameters =
        TerraformDestroyStepParameters.infoBuilder()
            .configuration(TerraformStepConfigurationParameters.builder()
                               .type(TerraformStepConfigurationType.INLINE)
                               .spec(TerraformExecutionDataParameters.builder().configFiles(configFilesWrapper).build())
                               .build())
            .build();
    try {
      helper.validateDestroyStepConfigFilesInline(stepParameters);
    } catch (GeneralException generalException) {
      assertThat(generalException.getMessage()).isEqualTo("Destroy Step Spec does not have Config files store");
    }
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testParseTerraformOutputs() {
    String terraformOutputString = "demo";

    Map<String, Object> response = helper.parseTerraformOutputs(terraformOutputString);
    assertThat(response.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testSaveRollbackDestroyConfigInlineOtherStore() {
    Ambiance ambiance = getAmbiance();
    TerraformConfigFilesWrapper configFilesWrapper = new TerraformConfigFilesWrapper();
    configFilesWrapper.setStore(StoreConfigWrapper.builder()
                                    .spec(BitbucketStore.builder()
                                              .branch(ParameterField.createValueField("master"))
                                              .gitFetchType(FetchType.BRANCH)
                                              .folderPath(ParameterField.createValueField("Config/"))
                                              .build())
                                    .type(StoreConfigType.BITBUCKET)
                                    .build());
    RemoteTerraformVarFileSpec remoteTerraformVarFileSpec = new RemoteTerraformVarFileSpec();
    remoteTerraformVarFileSpec.setStore(StoreConfigWrapper.builder()
                                            .spec(GitStore.builder()
                                                      .branch(ParameterField.createValueField("master"))
                                                      .gitFetchType(FetchType.BRANCH)
                                                      .folderPath(ParameterField.createValueField("VarFiles/"))
                                                      .build())
                                            .type(StoreConfigType.GIT)
                                            .build());
    LinkedHashMap<String, TerraformVarFile> varFilesMap = new LinkedHashMap<>();
    varFilesMap.put("var-file-1",
        TerraformVarFile.builder().identifier("var-file-1").type("Inline").spec(remoteTerraformVarFileSpec).build());
    TerraformApplyStepParameters parameters = TerraformApplyStepParameters.infoBuilder()
                                                  .provisionerIdentifier(ParameterField.createValueField("provId_$"))
                                                  .configuration(TerraformStepConfigurationParameters.builder()
                                                                     .type(TerraformStepConfigurationType.INLINE)
                                                                     .spec(TerraformExecutionDataParameters.builder()
                                                                               .configFiles(configFilesWrapper)
                                                                               .varFiles(varFilesMap)
                                                                               .build())
                                                                     .build())
                                                  .build();
    TerraformTaskNGResponse response =
        TerraformTaskNGResponse.builder()
            .commitIdForConfigFilesMap(ImmutableMap.of(TerraformStepHelper.TF_CONFIG_FILES, "commit-1",
                String.format(TerraformStepHelper.TF_VAR_FILES, 1), "commit-2"))
            .build();
    helper.saveRollbackDestroyConfigInline(parameters, response, ambiance);
    ArgumentCaptor<TerraformConfig> captor = ArgumentCaptor.forClass(TerraformConfig.class);
    verify(terraformConfigDAL).saveTerraformConfig(captor.capture());
    TerraformConfig config = captor.getValue();
    assertThat(config).isNotNull();
    GitStoreConfigDTO configFiles = config.getConfigFiles();
    assertThat(configFiles instanceof BitBucketStoreDTO).isTrue();
    BitBucketStoreDTO bitBucketStoreDTO = (BitBucketStoreDTO) configFiles;
    assertThat(bitBucketStoreDTO.getGitFetchType()).isEqualTo(FetchType.COMMIT);
    assertThat(bitBucketStoreDTO.getCommitId()).isEqualTo("commit-1");
    List<TerraformVarFileConfig> varFileConfigs = config.getVarFileConfigs();
    assertThat(varFileConfigs).isNotNull();
    assertThat(varFileConfigs.size()).isEqualTo(1);
    TerraformVarFileConfig terraformVarFileConfig = varFileConfigs.get(0);
    assertThat(terraformVarFileConfig instanceof TerraformRemoteVarFileConfig).isTrue();
    TerraformRemoteVarFileConfig remoteVarFileConfig = (TerraformRemoteVarFileConfig) terraformVarFileConfig;
    GitStoreConfigDTO gitStoreConfigDTO = remoteVarFileConfig.getGitStoreConfigDTO();
    assertThat(gitStoreConfigDTO instanceof GitStoreDTO).isTrue();
    GitStoreDTO gitStoreDTO = (GitStoreDTO) gitStoreConfigDTO;
    assertThat(gitStoreDTO.getGitFetchType()).isEqualTo(FetchType.COMMIT);
    assertThat(gitStoreDTO.getCommitId()).isEqualTo("commit-2");
  }
}

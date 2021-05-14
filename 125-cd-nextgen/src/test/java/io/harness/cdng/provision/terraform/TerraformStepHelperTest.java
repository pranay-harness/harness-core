package io.harness.cdng.provision.terraform;

import static io.harness.rule.OwnerRule.SATYAM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.fileservice.FileServiceClientFactory;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitLabStoreDTO;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.GitStoreConfigDTO;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.GithubStoreDTO;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.task.terraform.InlineTerraformVarFileInfo;
import io.harness.delegate.task.terraform.RemoteTerraformVarFileInfo;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.delegate.task.terraform.TerraformVarFileInfo;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ngpipeline.common.ParameterFieldHelper;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.dto.LocalConfigDTO;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptionType;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.CDP)
public class TerraformStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private HPersistence mockPersistence;
  @Mock private K8sStepHelper mockK8sStepHelper;
  @Mock private ExecutionSweepingOutputService mockExecutionSweepingOutputService;
  @Mock private GitConfigAuthenticationInfoHelper mockGitConfigAuthenticationInfoHelper;
  @Mock private FileServiceClientFactory mockFileService;
  @Mock private SecretManagerClientService mockSecretManagerClientService;
  @InjectMocks private TerraformStepHelper helper;

  private Ambiance getAmbiance() {
    return Ambiance.newBuilder()
        .putSetupAbstractions("accountId", "test-account")
        .putSetupAbstractions("projectIdentifier", "test-project")
        .putSetupAbstractions("orgIdentifier", "test-org")
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
                                    .type("Github")
                                    .build());
    InlineTerraformVarFileSpec inlineTerraformVarFileSpec = new InlineTerraformVarFileSpec();
    inlineTerraformVarFileSpec.setContent(ParameterField.createValueField("var-content"));
    InlineTerraformBackendConfigSpec inlineTerraformBackendConfigSpec = new InlineTerraformBackendConfigSpec();
    inlineTerraformBackendConfigSpec.setContent(ParameterField.createValueField("back-content"));
    TerraformBackendConfig terraformBackendConfig = new TerraformBackendConfig();
    terraformBackendConfig.setTerraformBackendConfigSpec(inlineTerraformBackendConfigSpec);
    TerraformPlanStepParameters planStepParameters =
        TerraformPlanStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("provId"))
            .configuration(TerraformPlanExecutionDataParameters.builder()
                               .configFiles(configFilesWrapper)
                               .command(TerraformPlanCommand.APPLY)
                               .secretManagerRef(ParameterField.createValueField("secret"))
                               .varFiles(ImmutableMap.of("var-file-1",
                                   TerraformVarFile.builder()
                                       .identifier("var-file-1")
                                       .type("Inline")
                                       .spec(inlineTerraformVarFileSpec)
                                       .build()))
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
                                    .type("Github")
                                    .build());
    RemoteTerraformVarFileSpec remoteTerraformVarFileSpec = new RemoteTerraformVarFileSpec();
    remoteTerraformVarFileSpec.setStoreConfigWrapper(
        StoreConfigWrapper.builder()
            .spec(GitLabStore.builder()
                      .branch(ParameterField.createValueField("master"))
                      .gitFetchType(FetchType.BRANCH)
                      .folderPath(ParameterField.createValueField("VarFiles/"))
                      .build())
            .type("GitLab")
            .build());
    TerraformApplyStepParameters parameters = TerraformApplyStepParameters.infoBuilder()
                                                  .configuration(TerrformStepConfigurationParameters.builder()
                                                                     .type(TerraformStepConfigurationType.INLINE)
                                                                     .spec(TerraformExecutionDataParameters.builder()
                                                                               .configFiles(configFilesWrapper)
                                                                               .varFiles(ImmutableMap.of("var-file-1",
                                                                                   TerraformVarFile.builder()
                                                                                       .identifier("var-file-1")
                                                                                       .type("Inline")
                                                                                       .spec(remoteTerraformVarFileSpec)
                                                                                       .build()))
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
    verify(mockPersistence).save(captor.capture());
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
    remoteTerraformVarFileSpec.setStoreConfigWrapper(
        StoreConfigWrapper.builder()
            .spec(GitLabStore.builder()
                      .branch(ParameterField.createValueField("master"))
                      .gitFetchType(FetchType.BRANCH)
                      .paths(ParameterField.createValueField(Collections.singletonList("VarFiles/")))
                      .connectorRef(ParameterField.createValueField("ConnectorRef"))
                      .build())
            .type("GitLab")
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
}

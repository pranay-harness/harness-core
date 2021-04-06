package io.harness.cdng.provision.terraform;

import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.validation.Validator.notEmptyCheck;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.provision.terraform.TerraformConfig.TerraformConfigBuilder;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.ngpipeline.common.ParameterFieldHelper;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.security.encryption.EncryptedDataDetail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@OwnedBy(HarnessTeam.CDP)
@Slf4j
@Singleton
public class TerraformStepHelper {
  private static final String INHERIT_OUTPUT_FORMAT = "tfInheritOutput_%s";
  public static final String TF_CONFIG_FILES = "TF_CONFIG_FILES";
  public static final String TF_VAR_FILES = "TF_VAR_FILES_%d";

  @Inject private HPersistence persistence;
  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;

  public String generateFullIdentifier(String provisionerIdentifier, Ambiance ambiance) {
    return String.format("%s/%s/%s/%s", AmbianceHelper.getAccountId(ambiance),
        AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance),
        provisionerIdentifier);
  }

  public GitFetchFilesConfig getGitFetchFilesConfig(StoreConfig store, Ambiance ambiance, String identifier) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = k8sStepHelper.getConnector(connectorId, ambiance);
    String validationMessage = String.format("Invalid type for manifestType: [%s]", identifier);
    k8sStepHelper.validateManifest(store.getKind(), connectorDTO, validationMessage);
    GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
    NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
    SSHKeySpecDTO sshKeySpecDTO =
        gitConfigAuthenticationInfoHelper.getSSHKey(gitConfigDTO, AmbianceHelper.getAccountId(ambiance),
            AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance));
    List<EncryptedDataDetail> encryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);
    String repoName = gitStoreConfig.getRepoName() != null ? gitStoreConfig.getRepoName().getValue() : null;
    if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT) {
      String repoUrl = getGitRepoUrl(gitConfigDTO, repoName);
      gitConfigDTO.setUrl(repoUrl);
      gitConfigDTO.setGitConnectionType(GitConnectionType.REPO);
    }
    List<String> paths = new ArrayList<>();
    if (TF_CONFIG_FILES.equals(identifier)) {
      paths.add(ParameterFieldHelper.getParameterFieldValue(gitStoreConfig.getFolderPath()));
    } else {
      paths.addAll(ParameterFieldHelper.getParameterFieldValue(gitStoreConfig.getPaths()));
    }
    GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder()
                                                        .gitConfigDTO(gitConfigDTO)
                                                        .sshKeySpecDTO(sshKeySpecDTO)
                                                        .encryptedDataDetails(encryptedDataDetails)
                                                        .fetchType(gitStoreConfig.getGitFetchType())
                                                        .branch(getParameterFieldValue(gitStoreConfig.getBranch()))
                                                        .commitId(getParameterFieldValue(gitStoreConfig.getCommitId()))
                                                        .paths(paths)
                                                        .connectorName(connectorDTO.getName())
                                                        .build();

    return GitFetchFilesConfig.builder()
        .identifier(identifier)
        .succeedIfFileNotFound(false)
        .gitStoreDelegateConfig(gitStoreDelegateConfig)
        .build();
  }

  private String getGitRepoUrl(GitConfigDTO gitConfigDTO, String repoName) {
    repoName = trimToEmpty(repoName);
    notEmptyCheck("Repo name cannot be empty for Account level git connector", repoName);
    String purgedRepoUrl = gitConfigDTO.getUrl().replaceAll("/*$", "");
    String purgedRepoName = repoName.replaceAll("^/*", "");
    return purgedRepoUrl + "/" + purgedRepoName;
  }

  public TerraformInheritOutput getSavedInheritOutput(String provisionerIdentifier, Ambiance ambiance) {
    String fullEntityId = generateFullIdentifier(provisionerIdentifier, ambiance);
    String inheritOutputName = String.format(INHERIT_OUTPUT_FORMAT, fullEntityId);
    OptionalSweepingOutput output = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(inheritOutputName));
    if (!output.isFound()) {
      throw new InvalidRequestException(String.format("Terraform inherit output: [%s] not found", inheritOutputName));
    }

    return (TerraformInheritOutput) output.getOutput();
  }

  public Map<String, String> getEnvironmentVariablesMap(Map<String, Object> inputVariables) {
    if (EmptyPredicate.isEmpty(inputVariables)) {
      return new HashMap<>();
    }
    Map<String, String> res = new LinkedHashMap<>();
    inputVariables.keySet().forEach(
        key -> res.put(key, ((ParameterField<?>) inputVariables.get(key)).getValue().toString()));
    return res;
  }

  private StoreConfig getStoreConfigAtCommitId(StoreConfig storeConfig, String commitId) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig.cloneInternal();
    if (EmptyPredicate.isEmpty(commitId) || FetchType.COMMIT == gitStoreConfig.getGitFetchType()) {
      return gitStoreConfig;
    }
    ParameterField<String> commitIdField = ParameterField.createValueField(commitId);
    switch (storeConfig.getKind()) {
      case ManifestStoreType.BITBUCKET: {
        BitbucketStore bitbucketStore = (BitbucketStore) gitStoreConfig;
        bitbucketStore.setGitFetchType(FetchType.COMMIT);
        bitbucketStore.setCommitId(commitIdField);
        break;
      }
      case ManifestStoreType.GITLAB: {
        GitLabStore gitLabStore = (GitLabStore) gitStoreConfig;
        gitLabStore.setGitFetchType(FetchType.COMMIT);
        gitLabStore.setCommitId(commitIdField);
        break;
      }
      case ManifestStoreType.GIT: {
        GitStore gitStore = (GitStore) gitStoreConfig;
        gitStore.setGitFetchType(FetchType.COMMIT);
        gitStore.setCommitId(commitIdField);
        break;
      }
      case ManifestStoreType.GITHUB: {
        GithubStore githubStore = (GithubStore) gitStoreConfig;
        githubStore.setGitFetchType(FetchType.COMMIT);
        githubStore.setCommitId(commitIdField);
        break;
      }
      default: {
        log.warn(String.format("Unknown store kind: [%s]", storeConfig.getKind()));
        break;
      }
    }
    return gitStoreConfig;
  }

  public void saveRollbackDestroyConfigInline(
      TerraformApplyStepParameters stepParameters, TerraformTaskNGResponse response, Ambiance ambiance) {
    TerraformConfigBuilder builder =
        TerraformConfig.builder()
            .accountId(AmbianceHelper.getAccountId(ambiance))
            .orgId(AmbianceHelper.getOrgIdentifier(ambiance))
            .projectId(AmbianceHelper.getProjectIdentifier(ambiance))
            .entityId(generateFullIdentifier(stepParameters.getProvisionerIdentifier(), ambiance))
            .pipelineExecutionId(ambiance.getPlanExecutionId())
            .createdAt(System.currentTimeMillis());

    Map<String, String> commitIdMap = response.getCommitIdForConfigFilesMap();
    builder.configFiles(getStoreConfigAtCommitId(
        stepParameters.getConfigFilesWrapper().getStoreConfig(), commitIdMap.get(TF_CONFIG_FILES)));
    List<StoreConfigWrapper> remoteVarFiles = stepParameters.getRemoteVarFiles();
    if (EmptyPredicate.isNotEmpty(remoteVarFiles)) {
      int i = 1;
      List<StoreConfig> remoteVarFilesAtCommitIds = new ArrayList<>();
      for (StoreConfigWrapper varFileWrapper : stepParameters.getRemoteVarFiles()) {
        remoteVarFilesAtCommitIds.add(
            getStoreConfigAtCommitId(varFileWrapper.getStoreConfig(), commitIdMap.get(String.format(TF_VAR_FILES, i))));
        i++;
      }
      builder.remoteVarFiles(remoteVarFilesAtCommitIds);
    }
    builder.inlineVarFiles(ParameterFieldHelper.getParameterFieldValue(stepParameters.getInlineVarFiles()))
        .backendConfig(ParameterFieldHelper.getParameterFieldValue(stepParameters.getBackendConfig()))
        .environmentVariables(getEnvironmentVariablesMap(stepParameters.getEnvironmentVariables()))
        .workspace(ParameterFieldHelper.getParameterFieldValue(stepParameters.getWorkspace()))
        .targets(ParameterFieldHelper.getParameterFieldValue(stepParameters.getTargets()));
    persistence.save(builder.build());
  }

  public Map<String, Object> parseTerraformOutputs(String terraformOutputString) {
    Map<String, Object> outputs = new LinkedHashMap<>();
    if (EmptyPredicate.isEmpty(terraformOutputString)) {
      return outputs;
    }
    try {
      TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
      Map<String, Object> json = new ObjectMapper().readValue(IOUtils.toInputStream(terraformOutputString), typeRef);

      json.forEach((key, object) -> outputs.put(key, ((Map<String, Object>) object).get("value")));

    } catch (IOException exception) {
      log.error("", exception);
    }
    return outputs;
  }
}

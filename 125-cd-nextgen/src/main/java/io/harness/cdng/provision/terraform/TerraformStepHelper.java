package io.harness.cdng.provision.terraform;

import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.provision.TerraformConstants.TF_DESTROY_NAME_PREFIX;
import static io.harness.provision.TerraformConstants.TF_NAME_PREFIX;
import static io.harness.validation.Validator.notEmptyCheck;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.fileservice.FileServiceClientFactory;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.BitbucketStore;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.GitStoreConfigDTO;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.cdng.provision.terraform.TerraformConfig.TerraformConfigBuilder;
import io.harness.cdng.provision.terraform.TerraformConfig.TerraformConfigKeys;
import io.harness.cdng.provision.terraform.TerraformInheritOutput.TerraformInheritOutputBuilder;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.InlineTerraformVarFileInfo;
import io.harness.delegate.task.terraform.RemoteTerraformVarFileInfo;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.delegate.task.terraform.TerraformVarFileInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.mappers.SecretManagerConfigMapper;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.ngpipeline.common.ParameterFieldHelper;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.remote.client.RestClientUtils;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.utils.IdentifierRefHelper;
import io.harness.validation.Validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.mongodb.morphia.query.Sort;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class TerraformStepHelper {
  private static final String INHERIT_OUTPUT_FORMAT = "tfInheritOutput_%s";
  public static final String TF_CONFIG_FILES = "TF_CONFIG_FILES";
  public static final String TF_VAR_FILES = "TF_VAR_FILES_%d";

  @Inject private HPersistence persistence;
  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Inject private FileServiceClientFactory fileService;
  @Inject private SecretManagerClientService secretManagerClientService;

  public String generateFullIdentifier(String provisionerIdentifier, Ambiance ambiance) {
    return String.format("%s/%s/%s/%s", AmbianceHelper.getAccountId(ambiance),
        AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance),
        provisionerIdentifier);
  }

  private void validateGitStoreConfig(GitStoreConfig gitStoreConfig) {
    Validator.notNullCheck("Git Store Config is null", gitStoreConfig);
    FetchType gitFetchType = gitStoreConfig.getGitFetchType();
    switch (gitFetchType) {
      case BRANCH:
        Validator.notEmptyCheck("Branch is Empty in Git Store config",
            ParameterFieldHelper.getParameterFieldValue(gitStoreConfig.getBranch()));
        break;
      case COMMIT:
        Validator.notEmptyCheck("Commit Id is Empty in Git Store config",
            ParameterFieldHelper.getParameterFieldValue(gitStoreConfig.getCommitId()));
        break;
      default:
        throw new InvalidRequestException(String.format("Unrecognized git fetch type: [%s]", gitFetchType.name()));
    }
  }

  public GitFetchFilesConfig getGitFetchFilesConfig(StoreConfig store, Ambiance ambiance, String identifier) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    validateGitStoreConfig(gitStoreConfig);
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = k8sStepHelper.getConnector(connectorId, ambiance);
    String validationMessage = String.format("Invalid type for manifestType: [%s]", identifier);
    // TODO: fix manifest part, remove k8s dependency
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
      throw new InvalidRequestException(
          String.format("Did not find any Plan step for provisioner identifier: [%s]", provisionerIdentifier));
    }

    return (TerraformInheritOutput) output.getOutput();
  }

  public void saveTerraformInheritOutput(TerraformPlanStepParameters planStepParameters,
      TerraformTaskNGResponse terraformTaskNGResponse, Ambiance ambiance) {
    validatePlanStepConfigFiles(planStepParameters);
    TerraformPlanExecutionDataParameters configuration = planStepParameters.getConfiguration();
    TerraformInheritOutputBuilder builder = TerraformInheritOutput.builder().workspace(
        ParameterFieldHelper.getParameterFieldValue(configuration.getWorkspace()));
    Map<String, String> commitIdMap = terraformTaskNGResponse.getCommitIdForConfigFilesMap();
    builder
        .configFiles(getStoreConfigAtCommitId(
            configuration.getConfigFiles().getStore().getSpec(), commitIdMap.get(TF_CONFIG_FILES)))
        .varFileConfigs(toTerraformVarFileConfig(configuration.getVarFiles(), terraformTaskNGResponse, ambiance))
        .backendConfig(getBackendConfig(configuration.getBackendConfig()))
        .environmentVariables(getEnvironmentVariablesMap(configuration.getEnvironmentVariables()))
        .targets(ParameterFieldHelper.getParameterFieldValue(configuration.getTargets()))
        .encryptedTfPlan(terraformTaskNGResponse.getEncryptedTfPlan())
        .encryptionConfig(getEncryptionConfig(ambiance, planStepParameters))
        .planName(getTerraformPlanName(planStepParameters.getConfiguration().getCommand(), ambiance));
    String fullEntityId = generateFullIdentifier(
        ParameterFieldHelper.getParameterFieldValue(planStepParameters.getProvisionerIdentifier()), ambiance);
    String inheritOutputName = String.format(INHERIT_OUTPUT_FORMAT, fullEntityId);
    executionSweepingOutputService.consume(ambiance, inheritOutputName, builder.build(), StepOutcomeGroup.STAGE.name());
  }

  private String getTerraformPlanName(TerraformPlanCommand terraformPlanCommand, Ambiance ambiance) {
    String prefix = TerraformPlanCommand.DESTROY == terraformPlanCommand ? TF_DESTROY_NAME_PREFIX : TF_NAME_PREFIX;
    return String.format(prefix, ambiance.getPlanExecutionId());
  }

  public EncryptionConfig getEncryptionConfig(Ambiance ambiance, TerraformPlanStepParameters planStepParameters) {
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        ParameterFieldHelper.getParameterFieldValue(planStepParameters.getConfiguration().getSecretManagerRef()),
        AmbianceHelper.getAccountId(ambiance), AmbianceHelper.getOrgIdentifier(ambiance),
        AmbianceHelper.getProjectIdentifier(ambiance));

    return SecretManagerConfigMapper.fromDTO(secretManagerClientService.getSecretManager(
        identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(),
        identifierRef.getIdentifier(), false));
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

  private GitStoreConfig getStoreConfigAtCommitId(StoreConfig storeConfig, String commitId) {
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

  public void saveRollbackDestroyConfigInherited(TerraformApplyStepParameters stepParameters, Ambiance ambiance) {
    TerraformInheritOutput inheritOutput = getSavedInheritOutput(
        ParameterFieldHelper.getParameterFieldValue(stepParameters.getProvisionerIdentifier()), ambiance);

    persistence.save(
        TerraformConfig.builder()
            .accountId(AmbianceHelper.getAccountId(ambiance))
            .orgId(AmbianceHelper.getOrgIdentifier(ambiance))
            .projectId(AmbianceHelper.getProjectIdentifier(ambiance))
            .entityId(generateFullIdentifier(
                ParameterFieldHelper.getParameterFieldValue(stepParameters.getProvisionerIdentifier()), ambiance))
            .pipelineExecutionId(ambiance.getPlanExecutionId())
            .configFiles(inheritOutput.getConfigFiles().toGitStoreConfigDTO())
            .varFileConfigs(inheritOutput.getVarFileConfigs())
            .backendConfig(inheritOutput.getBackendConfig())
            .environmentVariables(inheritOutput.getEnvironmentVariables())
            .workspace(inheritOutput.getWorkspace())
            .targets(inheritOutput.getTargets())
            .build());
  }

  public void validateApplyStepParamsInline(TerraformApplyStepParameters stepParameters) {
    Validator.notNullCheck("Apply Step Parameters are null", stepParameters);
    Validator.notNullCheck("Apply Step configuration is NULL", stepParameters.getConfiguration());
  }

  public void validateApplyStepConfigFilesInline(TerraformApplyStepParameters stepParameters) {
    Validator.notNullCheck("Apply Step Parameters are null", stepParameters);
    Validator.notNullCheck("Apply Step configuration is NULL", stepParameters.getConfiguration());
    TerraformExecutionDataParameters spec = stepParameters.getConfiguration().getSpec();
    Validator.notNullCheck("Apply Step Spec is NULL", spec);
    Validator.notNullCheck("Apply Step Spec does not have Config files", spec.getConfigFiles());
    Validator.notNullCheck("Apply Step Spec does not have Config files store", spec.getConfigFiles().getStore());
  }

  public void validateDestroyStepParamsInline(TerraformDestroyStepParameters stepParameters) {
    Validator.notNullCheck("Destroy Step Parameters are null", stepParameters);
    Validator.notNullCheck("Destroy Step configuration is NULL", stepParameters.getConfiguration());
  }

  public void validateDestroyStepConfigFilesInline(TerraformDestroyStepParameters stepParameters) {
    Validator.notNullCheck("Destroy Step Parameters are null", stepParameters);
    Validator.notNullCheck("Destroy Step configuration is NULL", stepParameters.getConfiguration());
    TerraformExecutionDataParameters spec = stepParameters.getConfiguration().getSpec();
    Validator.notNullCheck("Destroy Step Spec is NULL", spec);
    Validator.notNullCheck("Destroy Step Spec does not have Config files", spec.getConfigFiles());
    Validator.notNullCheck("Destroy Step Spec does not have Config files store", spec.getConfigFiles().getStore());
  }

  public void validatePlanStepConfigFiles(TerraformPlanStepParameters stepParameters) {
    Validator.notNullCheck("Plan Step Parameters are null", stepParameters);
    Validator.notNullCheck("Plan Step configuration is NULL", stepParameters.getConfiguration());
    Validator.notNullCheck("Plan Step does not have Config files", stepParameters.getConfiguration().getConfigFiles());
    Validator.notNullCheck(
        "Plan Step does not have Config files store", stepParameters.getConfiguration().getConfigFiles().getStore());
    Validator.notNullCheck("Plan Step does not have Plan Command", stepParameters.getConfiguration().getCommand());
    Validator.notNullCheck(
        "Plan Step does not have Secret Manager Ref", stepParameters.getConfiguration().getSecretManagerRef());
  }

  public void saveRollbackDestroyConfigInline(
      TerraformApplyStepParameters stepParameters, TerraformTaskNGResponse response, Ambiance ambiance) {
    validateApplyStepConfigFilesInline(stepParameters);
    TerraformStepConfigurationParameters configuration = stepParameters.getConfiguration();
    TerraformExecutionDataParameters spec = configuration.getSpec();
    TerraformConfigBuilder builder =
        TerraformConfig.builder()
            .accountId(AmbianceHelper.getAccountId(ambiance))
            .orgId(AmbianceHelper.getOrgIdentifier(ambiance))
            .projectId(AmbianceHelper.getProjectIdentifier(ambiance))
            .entityId(generateFullIdentifier(
                ParameterFieldHelper.getParameterFieldValue(stepParameters.getProvisionerIdentifier()), ambiance))
            .pipelineExecutionId(ambiance.getPlanExecutionId());

    Map<String, String> commitIdMap = response.getCommitIdForConfigFilesMap();
    builder
        .configFiles(
            getStoreConfigAtCommitId(spec.getConfigFiles().getStore().getSpec(), commitIdMap.get(TF_CONFIG_FILES))
                .toGitStoreConfigDTO())
        .varFileConfigs(toTerraformVarFileConfig(spec.getVarFiles(), response, ambiance))
        .backendConfig(getBackendConfig(spec.getBackendConfig()))
        .environmentVariables(getEnvironmentVariablesMap(spec.getEnvironmentVariables()))
        .workspace(ParameterFieldHelper.getParameterFieldValue(spec.getWorkspace()))
        .targets(ParameterFieldHelper.getParameterFieldValue(spec.getTargets()));
    persistence.save(builder.build());
  }

  public String getBackendConfig(TerraformBackendConfig backendConfig) {
    if (backendConfig != null) {
      TerraformBackendConfigSpec terraformBackendConfigSpec = backendConfig.getTerraformBackendConfigSpec();
      if (terraformBackendConfigSpec instanceof InlineTerraformBackendConfigSpec) {
        return ParameterFieldHelper.getParameterFieldValue(
            ((InlineTerraformBackendConfigSpec) terraformBackendConfigSpec).getContent());
      }
    }
    return null;
  }

  public TerraformConfig getLastSuccessfulApplyConfig(TerraformDestroyStepParameters parameters, Ambiance ambiance) {
    String entityId = generateFullIdentifier(
        ParameterFieldHelper.getParameterFieldValue(parameters.getProvisionerIdentifier()), ambiance);
    TerraformConfig terraformConfig =
        persistence.createQuery(TerraformConfig.class)
            .filter(TerraformConfigKeys.accountId, AmbianceHelper.getAccountId(ambiance))
            .filter(TerraformConfigKeys.orgId, AmbianceHelper.getOrgIdentifier(ambiance))
            .filter(TerraformConfigKeys.projectId, AmbianceHelper.getProjectIdentifier(ambiance))
            .filter(TerraformConfigKeys.entityId, entityId)
            .order(Sort.descending(TerraformConfigKeys.createdAt))
            .get();
    if (terraformConfig == null) {
      throw new InvalidRequestException(String.format("Terraform config for Last Apply not found: [%s]", entityId));
    }
    return terraformConfig;
  }

  public void clearTerraformConfig(Ambiance ambiance, String entityId) {
    persistence.delete(persistence.createQuery(TerraformConfig.class)
                           .filter(TerraformConfigKeys.accountId, AmbianceHelper.getAccountId(ambiance))
                           .filter(TerraformConfigKeys.orgId, AmbianceHelper.getOrgIdentifier(ambiance))
                           .filter(TerraformConfigKeys.projectId, AmbianceHelper.getProjectIdentifier(ambiance))
                           .filter(TerraformConfigKeys.entityId, entityId));
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

  public String getLatestFileId(String entityId) {
    try {
      return RestClientUtils.getResponse(fileService.get().getLatestFileId(entityId, FileBucket.TERRAFORM_STATE));
    } catch (Exception exception) {
      String message = String.format("Unable to call fileservice to fetch latest file id for entityId: [%s]", entityId);
      throw new InvalidRequestException(message, exception);
    }
  }

  public void saveTerraformConfig(TerraformConfig rollbackConfig, Ambiance ambiance) {
    persistence.save(TerraformConfig.builder()
                         .accountId(AmbianceHelper.getAccountId(ambiance))
                         .orgId(AmbianceHelper.getOrgIdentifier(ambiance))
                         .projectId(AmbianceHelper.getProjectIdentifier(ambiance))
                         .entityId(rollbackConfig.getEntityId())
                         .pipelineExecutionId(ambiance.getPlanExecutionId())
                         .configFiles(rollbackConfig.getConfigFiles())
                         .varFileConfigs(rollbackConfig.getVarFileConfigs())
                         .backendConfig(rollbackConfig.getBackendConfig())
                         .environmentVariables(rollbackConfig.getEnvironmentVariables())
                         .workspace(rollbackConfig.getWorkspace())
                         .targets(rollbackConfig.getTargets())
                         .build());
  }

  public void updateParentEntityIdAndVersion(String entityId, String stateFileId) {
    try {
      RestClientUtils.getResponse(fileService.get().updateParentEntityIdAndVersion(
          URLEncoder.encode(entityId, "UTF-8"), stateFileId, FileBucket.TERRAFORM_STATE));
    } catch (Exception ex) {
      throw new InvalidRequestException(
          String.format("Unable to update entityId and Version for entityId: [%s]", entityId));
    }
  }

  // Conversion Methods
  public List<TerraformVarFileInfo> toTerraformVarFileInfo(
      Map<String, TerraformVarFile> varFilesMap, Ambiance ambiance) {
    if (EmptyPredicate.isNotEmpty(varFilesMap)) {
      List<TerraformVarFileInfo> varFileInfo = new ArrayList<>();
      int i = 0;
      for (TerraformVarFile file : varFilesMap.values()) {
        if (file != null) {
          TerraformVarFileSpec spec = file.getSpec();
          if (spec instanceof InlineTerraformVarFileSpec) {
            String content =
                ParameterFieldHelper.getParameterFieldValue(((InlineTerraformVarFileSpec) spec).getContent());
            if (EmptyPredicate.isNotEmpty(content)) {
              varFileInfo.add(InlineTerraformVarFileInfo.builder().varFileContent(content).build());
            }
          } else if (spec instanceof RemoteTerraformVarFileSpec) {
            StoreConfigWrapper storeConfigWrapper = ((RemoteTerraformVarFileSpec) spec).getStore();
            if (storeConfigWrapper != null) {
              i++;
              StoreConfig storeConfig = storeConfigWrapper.getSpec();
              GitFetchFilesConfig gitFetchFilesConfig =
                  getGitFetchFilesConfig(storeConfig, ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i));
              varFileInfo.add(RemoteTerraformVarFileInfo.builder().gitFetchFilesConfig(gitFetchFilesConfig).build());
            }
          }
        }
      }
      return varFileInfo;
    }
    return Collections.emptyList();
  }

  public List<TerraformVarFileConfig> toTerraformVarFileConfig(
      Map<String, TerraformVarFile> varFilesMap, TerraformTaskNGResponse response, Ambiance ambiance) {
    if (EmptyPredicate.isNotEmpty(varFilesMap)) {
      List<TerraformVarFileConfig> varFileConfigs = new ArrayList<>();
      int i = 0;
      for (TerraformVarFile file : varFilesMap.values()) {
        if (file != null) {
          TerraformVarFileSpec spec = file.getSpec();
          if (spec instanceof InlineTerraformVarFileSpec) {
            String content =
                ParameterFieldHelper.getParameterFieldValue(((InlineTerraformVarFileSpec) spec).getContent());
            if (EmptyPredicate.isNotEmpty(content)) {
              varFileConfigs.add(TerraformInlineVarFileConfig.builder().varFileContent(content).build());
            }
          } else if (spec instanceof RemoteTerraformVarFileSpec) {
            StoreConfigWrapper storeConfigWrapper = ((RemoteTerraformVarFileSpec) spec).getStore();
            if (storeConfigWrapper != null) {
              i++;
              StoreConfig storeConfig = storeConfigWrapper.getSpec();
              GitStoreConfigDTO gitStoreConfigDTO = getStoreConfigAtCommitId(
                  storeConfig, response.getCommitIdForConfigFilesMap().get(String.format(TF_VAR_FILES, i)))
                                                        .toGitStoreConfigDTO();
              varFileConfigs.add(TerraformRemoteVarFileConfig.builder().gitStoreConfigDTO(gitStoreConfigDTO).build());
            }
          }
        }
      }
      return varFileConfigs;
    }
    return Collections.emptyList();
  }

  public List<TerraformVarFileInfo> prepareTerraformVarFileInfo(
      List<TerraformVarFileConfig> varFileConfigs, Ambiance ambiance) {
    if (EmptyPredicate.isNotEmpty(varFileConfigs)) {
      int i = 0;
      List<TerraformVarFileInfo> varFileInfo = new ArrayList<>();
      for (TerraformVarFileConfig fileConfig : varFileConfigs) {
        if (fileConfig instanceof TerraformInlineVarFileConfig) {
          varFileInfo.add(InlineTerraformVarFileInfo.builder()
                              .varFileContent(((TerraformInlineVarFileConfig) fileConfig).getVarFileContent())
                              .build());
        } else if (fileConfig instanceof TerraformRemoteVarFileConfig) {
          i++;
          GitStoreConfig gitStoreConfig =
              ((TerraformRemoteVarFileConfig) fileConfig).getGitStoreConfigDTO().toGitStoreConfig();
          GitFetchFilesConfig gitFetchFilesConfig =
              getGitFetchFilesConfig(gitStoreConfig, ambiance, String.format(TerraformStepHelper.TF_VAR_FILES, i));
          varFileInfo.add(RemoteTerraformVarFileInfo.builder().gitFetchFilesConfig(gitFetchFilesConfig).build());
        }
      }
      return varFileInfo;
    }
    return Collections.emptyList();
  }
}

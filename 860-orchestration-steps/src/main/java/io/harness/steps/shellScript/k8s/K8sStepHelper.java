package io.harness.steps.shellScript.k8s;

import static io.harness.steps.shellScript.yaml.InfrastructureKind.KUBERNETES_DIRECT;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.ngpipeline.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.steps.StepUtils.prepareTaskRequest;
import static io.harness.validation.Validator.notEmptyCheck;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.steps.shellScript.beans.InfrastructureOutcome;
import io.harness.steps.shellScript.beans.K8sDirectInfrastructureOutcome;
import io.harness.steps.shellScript.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.steps.shellScript.manifest.ManifestStoreType;
import io.harness.steps.shellScript.manifest.ManifestType;
import io.harness.steps.shellScript.manifest.yaml.GitStoreConfig;
import io.harness.steps.shellScript.manifest.yaml.HelmChartManifestOutcome;
import io.harness.steps.shellScript.manifest.yaml.HelmManifestCommandFlag;
import io.harness.steps.shellScript.manifest.yaml.HttpStoreConfig;
import io.harness.steps.shellScript.manifest.yaml.K8sManifestOutcome;
import io.harness.steps.shellScript.manifest.yaml.KustomizeManifestOutcome;
import io.harness.steps.shellScript.manifest.yaml.ManifestOutcome;
import io.harness.steps.shellScript.manifest.yaml.OpenshiftManifestOutcome;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.steps.shellScript.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.common.NGTaskType;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.delegate.task.k8s.OpenshiftManifestDelegateConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.helm.HelmSubCommandType;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.RollbackOutcome;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hibernate.validator.constraints.NotEmpty;

@Singleton
public class K8sStepHelper {
  private static final Set<String> K8S_SUPPORTED_MANIFEST_TYPES = ImmutableSet.of(
      ManifestType.K8Manifest, ManifestType.HelmChart, ManifestType.Kustomize, ManifestType.OpenshiftTemplate);

  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private OutcomeService outcomeService;
  @Inject GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Inject private EncryptionHelper encryptionHelper;

  String getReleaseName(InfrastructureOutcome infrastructure) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        return k8SDirectInfrastructure.getReleaseName();
      default:
        throw new UnsupportedOperationException(format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  public ConnectorInfoDTO getConnector(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorId, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(
          format("Connector not found for identifier : [%s]", connectorId), WingsException.USER);
    }
    return connectorDTO.get().getConnector();
  }

  private void validateManifest(String manifestStoreType, ConnectorInfoDTO connectorInfoDTO, String message) {
    switch (manifestStoreType) {
      case ManifestStoreType.GIT:
        if (!(connectorInfoDTO.getConnectorConfig() instanceof GitConfigDTO)) {
          throw new InvalidRequestException(format("Invalid connector selected in %s. Select Git connector", message));
        }
        break;
      case ManifestStoreType.GITHUB:
        if (!(connectorInfoDTO.getConnectorConfig() instanceof GithubConnectorDTO)) {
          throw new InvalidRequestException(
              format("Invalid connector selected in %s. Select Github connector", message));
        }
        break;
      case ManifestStoreType.GITLAB:
        if (!(connectorInfoDTO.getConnectorConfig() instanceof GitlabConnectorDTO)) {
          throw new InvalidRequestException(
              format("Invalid connector selected in %s. Select GitLab connector", message));
        }
        break;
      case ManifestStoreType.BITBUCKET:
        if (!(connectorInfoDTO.getConnectorConfig() instanceof BitbucketConnectorDTO)) {
          throw new InvalidRequestException(
              format("Invalid connector selected in %s. Select Bitbucket connector", message));
        }
        break;
      case ManifestStoreType.HTTP:
        if (!(connectorInfoDTO.getConnectorConfig() instanceof HttpHelmConnectorDTO)) {
          throw new InvalidRequestException(
              format("Invalid connector selected in %s. Select Http Helm connector", message));
        }
        break;
      default:
        throw new UnsupportedOperationException(format("Unknown manifest store type: [%s]", manifestStoreType));
    }
  }

  public ManifestDelegateConfig getManifestDelegateConfig(ManifestOutcome manifestOutcome, Ambiance ambiance) {
    switch (manifestOutcome.getType()) {
      case ManifestType.K8Manifest:
        K8sManifestOutcome k8sManifestOutcome = (K8sManifestOutcome) manifestOutcome;
        return K8sManifestDelegateConfig.builder()
            .storeDelegateConfig(getStoreDelegateConfig(
                k8sManifestOutcome.getStore(), ambiance, manifestOutcome.getType(), manifestOutcome.getType()))
            .build();

      case ManifestType.HelmChart:
        HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;
        return HelmChartManifestDelegateConfig.builder()
            .storeDelegateConfig(getStoreDelegateConfig(helmChartManifestOutcome.getStore(), ambiance,
                manifestOutcome.getType(), manifestOutcome.getType() + " manifest"))
            .chartName(helmChartManifestOutcome.getChartName())
            .chartVersion(helmChartManifestOutcome.getChartVersion())
            .helmVersion(helmChartManifestOutcome.getHelmVersion())
            .helmCommandFlag(getDelegateHelmCommandFlag(helmChartManifestOutcome.getCommandFlags()))
            .build();

      case ManifestType.Kustomize:
        KustomizeManifestOutcome kustomizeManifestOutcome = (KustomizeManifestOutcome) manifestOutcome;
        return KustomizeManifestDelegateConfig.builder()
            .storeDelegateConfig(getStoreDelegateConfig(kustomizeManifestOutcome.getStore(), ambiance,
                manifestOutcome.getType(), manifestOutcome.getType() + " manifest"))
            .pluginPath(kustomizeManifestOutcome.getPluginPath())
            .build();

      case ManifestType.OpenshiftTemplate:
        OpenshiftManifestOutcome openshiftManifestOutcome = (OpenshiftManifestOutcome) manifestOutcome;
        return OpenshiftManifestDelegateConfig.builder()
            .storeDelegateConfig(getStoreDelegateConfig(openshiftManifestOutcome.getStore(), ambiance,
                manifestOutcome.getType(), manifestOutcome.getType() + " manifest"))
            .build();

      default:
        throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestOutcome.getType()));
    }
  }

  public StoreDelegateConfig getStoreDelegateConfig(
      StoreConfig storeConfig, Ambiance ambiance, String manifestType, String validationErrorMessage) {
    if (ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
      ConnectorInfoDTO connectorDTO = getConnector(getParameterFieldValue(gitStoreConfig.getConnectorRef()), ambiance);
      validateManifest(storeConfig.getKind(), connectorDTO, validationErrorMessage);

      GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
      NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
      SSHKeySpecDTO sshKeySpecDTO = getSshKeySpecDTO(gitConfigDTO, ambiance);
      List<EncryptedDataDetail> encryptedDataDetails =
          gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);
      return getGitStoreDelegateConfig(
          gitStoreConfig, connectorDTO, encryptedDataDetails, sshKeySpecDTO, gitConfigDTO, manifestType);
    }

    if (ManifestStoreType.HTTP.equals(storeConfig.getKind())) {
      HttpStoreConfig httpStoreConfig = (HttpStoreConfig) storeConfig;
      ConnectorInfoDTO helmConnectorDTO =
          getConnector(getParameterFieldValue(httpStoreConfig.getConnectorRef()), ambiance);
      validateManifest(storeConfig.getKind(), helmConnectorDTO, manifestType);

      return HttpHelmStoreDelegateConfig.builder()
          .repoName(helmConnectorDTO.getIdentifier())
          .repoDisplayName(helmConnectorDTO.getName())
          .httpHelmConnector((HttpHelmConnectorDTO) helmConnectorDTO.getConnectorConfig())
          .encryptedDataDetails(getEncryptionDataDetails(helmConnectorDTO, AmbianceHelper.getNgAccess(ambiance)))
          .build();
    }

    throw new UnsupportedOperationException(format("Unsupported Store Config type: [%s]", storeConfig.getKind()));
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(@Nonnull GitStoreConfig gitstoreConfig,
      @Nonnull ConnectorInfoDTO connectorDTO, @Nonnull List<EncryptedDataDetail> encryptedDataDetailList,
      SSHKeySpecDTO sshKeySpecDTO, @Nonnull GitConfigDTO gitConfigDTO, String manifestType) {
    convertToRepoGitConfig(gitstoreConfig, gitConfigDTO);
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO(gitConfigDTO)
        .sshKeySpecDTO(sshKeySpecDTO)
        .encryptedDataDetails(encryptedDataDetailList)
        .fetchType(gitstoreConfig.getGitFetchType())
        .branch(getParameterFieldValue(gitstoreConfig.getBranch()))
        .commitId(getParameterFieldValue(gitstoreConfig.getCommitId()))
        .paths(getPathsBasedOnManifest(gitstoreConfig, manifestType))
        .connectorName(connectorDTO.getName())
        .build();
  }

  private void convertToRepoGitConfig(GitStoreConfig gitstoreConfig, GitConfigDTO gitConfigDTO) {
    String repoName = gitstoreConfig.getRepoName() != null ? gitstoreConfig.getRepoName().getValue() : null;
    if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT) {
      String repoUrl = getGitRepoUrl(gitConfigDTO, repoName);
      gitConfigDTO.setUrl(repoUrl);
      gitConfigDTO.setGitConnectionType(GitConnectionType.REPO);
    }
  }

  private String getGitRepoUrl(GitConfigDTO gitConfigDTO, String repoName) {
    repoName = trimToEmpty(repoName);
    notEmptyCheck("Repo name cannot be empty for Account level git connector", repoName);
    String purgedRepoUrl = gitConfigDTO.getUrl().replaceAll("/*$", "");
    String purgedRepoName = repoName.replaceAll("^/*", "");
    return purgedRepoUrl + "/" + purgedRepoName;
  }

  private List<String> getPathsBasedOnManifest(GitStoreConfig gitstoreConfig, String manifestType) {
    List<String> paths = new ArrayList<>();
    switch (manifestType) {
      case ManifestType.HelmChart:
      case ManifestType.Kustomize:
        paths.add(getParameterFieldValue(gitstoreConfig.getFolderPath()));
        break;

      default:
        paths.addAll(getParameterFieldValue(gitstoreConfig.getPaths()));
    }

    return paths;
  }

  private SSHKeySpecDTO getSshKeySpecDTO(GitConfigDTO gitConfigDTO, Ambiance ambiance) {
    return gitConfigAuthenticationInfoHelper.getSSHKey(gitConfigDTO, AmbianceHelper.getAccountId(ambiance),
        AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance));
  }

  private List<EncryptedDataDetail> getEncryptionDataDetails(
      @Nonnull ConnectorInfoDTO connectorDTO, @Nonnull NGAccess ngAccess) {
    switch (connectorDTO.getConnectorType()) {
      case KUBERNETES_CLUSTER:
        KubernetesClusterConfigDTO connectorConfig = (KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig();
        if (connectorConfig.getCredential().getKubernetesCredentialType()
            == KubernetesCredentialType.MANUAL_CREDENTIALS) {
          KubernetesClusterDetailsDTO clusterDetailsDTO =
              (KubernetesClusterDetailsDTO) connectorConfig.getCredential().getConfig();

          KubernetesAuthCredentialDTO authCredentialDTO = clusterDetailsDTO.getAuth().getCredentials();
          return secretManagerClientService.getEncryptionDetails(ngAccess, authCredentialDTO);
        } else {
          return Collections.emptyList();
        }

      case HTTP_HELM_REPO:
        HttpHelmConnectorDTO httpHelmConnectorDTO = (HttpHelmConnectorDTO) connectorDTO.getConnectorConfig();
        List<DecryptableEntity> decryptableEntities = httpHelmConnectorDTO.getDecryptableEntities();
        if (isNotEmpty(decryptableEntities)) {
          return secretManagerClientService.getEncryptionDetails(ngAccess, decryptableEntities.get(0));
        } else {
          return Collections.emptyList();
        }

      case APP_DYNAMICS:
      case SPLUNK:
      case GIT:
      default:
        throw new UnsupportedOperationException(
            format("Unsupported connector type : [%s]", connectorDTO.getConnectorType()));
    }
  }

  public K8sInfraDelegateConfig getK8sInfraDelegateConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        ConnectorInfoDTO connectorDTO = getConnector(k8SDirectInfrastructure.getConnectorRef(), ambiance);

        return DirectK8sInfraDelegateConfig.builder()
            .namespace(k8SDirectInfrastructure.getNamespace())
            .kubernetesClusterConfigDTO((KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig())
            .encryptionDataDetails(getEncryptionDataDetails(connectorDTO, AmbianceHelper.getNgAccess(ambiance)))
            .build();

      default:
        throw new UnsupportedOperationException(
            format("Unsupported Infrastructure type: [%s]", infrastructure.getKind()));
    }
  }

  public List<EncryptedDataDetail> getEncryptedDataDetails(
      @Nonnull GitConfigDTO gitConfigDTO, @Nonnull Ambiance ambiance) {
    return secretManagerClientService.getEncryptionDetails(
        AmbianceHelper.getNgAccess(ambiance), gitConfigDTO.getGitAuth());
  }

  public TaskChainResponse queueK8sTask(K8sStepParameters k8sStepParameters, K8sDeployRequest k8sDeployRequest,
                                        Ambiance ambiance, InfrastructureOutcome infrastructure) {
    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {k8sDeployRequest})
                            .taskType(NGTaskType.K8S_COMMAND_TASK_NG.name())
                            .timeout(getTimeout(k8sStepParameters))
                            .async(true)
                            .build();

    final TaskRequest taskRequest =
        prepareTaskRequest(ambiance, taskData, kryoSerializer, k8sStepParameters.getCommandUnits());

    return TaskChainResponse.builder().taskRequest(taskRequest).chainEnd(true).passThroughData(infrastructure).build();
  }

  public List<String> renderValues(
      ManifestOutcome manifestOutcome, Ambiance ambiance, List<String> valuesFileContents) {
    if (isEmpty(valuesFileContents)) {
      return Collections.emptyList();
    }

    List<String> renderedValuesFileContents =
        valuesFileContents.stream()
            .map(valuesFileContent -> engineExpressionService.renderExpression(ambiance, valuesFileContent))
            .collect(Collectors.toList());

    if (manifestOutcome.getType() == ManifestType.OpenshiftTemplate) {
      Collections.reverse(renderedValuesFileContents);
    }

    return renderedValuesFileContents;
  }

  public TaskChainResponse executeValuesFetchTask(Ambiance ambiance, K8sStepParameters k8sStepParameters,
      InfrastructureOutcome infrastructure, ManifestOutcome k8sManifestOutcome,
      List<ValuesManifestOutcome> aggregatedValuesManifests) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs = new ArrayList<>();

    for (ValuesManifestOutcome valuesManifest : aggregatedValuesManifests) {
      if (ManifestStoreType.isInGitSubset(valuesManifest.getStore().getKind())) {
        GitStoreConfig gitStoreConfig = (GitStoreConfig) valuesManifest.getStore();
        String connectorId = gitStoreConfig.getConnectorRef().getValue();
        ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
        validateManifest(valuesManifest.getStore().getKind(), connectorDTO,
            format("Values YAML with Id [%s]", valuesManifest.getIdentifier()));

        GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
        NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
        SSHKeySpecDTO sshKeySpecDTO = getSshKeySpecDTO(gitConfigDTO, ambiance);
        List<EncryptedDataDetail> encryptedDataDetails =
            gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);

        GitStoreDelegateConfig gitStoreDelegateConfig = getGitStoreDelegateConfig(
            gitStoreConfig, connectorDTO, encryptedDataDetails, sshKeySpecDTO, gitConfigDTO, ManifestType.VALUES);

        gitFetchFilesConfigs.add(GitFetchFilesConfig.builder()
                                     .identifier(valuesManifest.getIdentifier())
                                     .succeedIfFileNotFound(false)
                                     .gitStoreDelegateConfig(gitStoreDelegateConfig)
                                     .build());
      }
    }

    String accountId = AmbianceHelper.getAccountId(ambiance);
    GitFetchRequest gitFetchRequest =
        GitFetchRequest.builder().gitFetchFilesConfigs(gitFetchFilesConfigs).accountId(accountId).build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(K8sStepHelper.getTimeout(k8sStepParameters))
                                  .taskType(NGTaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                                  .parameters(new Object[] {gitFetchRequest})
                                  .build();

    final TaskRequest taskRequest =
        prepareTaskRequest(ambiance, taskData, kryoSerializer, k8sStepParameters.getCommandUnits());

    K8sStepPassThroughData k8sStepPassThroughData = K8sStepPassThroughData.builder()
                                                        .k8sManifestOutcome(k8sManifestOutcome)
                                                        .valuesManifestOutcomes(aggregatedValuesManifests)
                                                        .infrastructure(infrastructure)
                                                        .build();
    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(k8sStepPassThroughData)
        .build();
  }

  public TaskChainResponse startChainLink(
          K8sStepExecutor k8sStepExecutor, Ambiance ambiance, K8sStepParameters k8sStepParameters) {
    ServiceOutcome serviceOutcome = (ServiceOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(YAMLFieldNameConstants.SERVICE));

    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(YAMLFieldNameConstants.INFRASTRUCTURE));

    Map<String, ManifestOutcome> manifestOutcomeMap = serviceOutcome.getManifestResults();
    if (isEmpty(manifestOutcomeMap) || isEmpty(manifestOutcomeMap.keySet())) {
      throw new InvalidRequestException("Manifests can't be empty");
    }

    ManifestOutcome k8sManifestOutcome = getK8sSupportedManifestOutcome(new LinkedList<>(manifestOutcomeMap.values()));
    List<ValuesManifestOutcome> aggregatedValuesManifests =
        getAggregatedValuesManifests(new LinkedList<>(manifestOutcomeMap.values()));

    if (isEmpty(aggregatedValuesManifests)) {
      return k8sStepExecutor.executeK8sTask(
          k8sManifestOutcome, ambiance, k8sStepParameters, Collections.emptyList(), infrastructureOutcome);
    }

    if (!isAnyRemoteStore(aggregatedValuesManifests)) {
      List<String> valuesFileContentsForLocalStore = getValuesFileContentsForLocalStore(aggregatedValuesManifests);
      return k8sStepExecutor.executeK8sTask(
          k8sManifestOutcome, ambiance, k8sStepParameters, valuesFileContentsForLocalStore, infrastructureOutcome);
    }

    return executeValuesFetchTask(
        ambiance, k8sStepParameters, infrastructureOutcome, k8sManifestOutcome, aggregatedValuesManifests);
  }

  @VisibleForTesting
  public ManifestOutcome getK8sSupportedManifestOutcome(@NotEmpty List<ManifestOutcome> manifestOutcomes) {
    List<ManifestOutcome> k8sManifests =
        manifestOutcomes.stream()
            .filter(manifestOutcome -> K8S_SUPPORTED_MANIFEST_TYPES.contains(manifestOutcome.getType()))
            .collect(Collectors.toList());
    if (isEmpty(k8sManifests)) {
      throw new InvalidRequestException("K8s Manifests are mandatory for k8s Rolling step", WingsException.USER);
    }

    if (k8sManifests.size() > 1) {
      throw new InvalidRequestException("There can be only a single K8s manifest", WingsException.USER);
    }
    return k8sManifests.get(0);
  }

  @VisibleForTesting
  public List<ValuesManifestOutcome> getAggregatedValuesManifests(@NotEmpty List<ManifestOutcome> manifestOutcomeList) {
    List<ValuesManifestOutcome> aggregateValuesManifests = new ArrayList<>();

    List<ValuesManifestOutcome> serviceValuesManifests =
        manifestOutcomeList.stream()
            .filter(manifestOutcome -> ManifestType.VALUES.equals(manifestOutcome.getType()))
            .map(manifestOutcome -> (ValuesManifestOutcome) manifestOutcome)
            .collect(Collectors.toList());

    if (isNotEmpty(serviceValuesManifests)) {
      aggregateValuesManifests.addAll(serviceValuesManifests);
    }
    return aggregateValuesManifests;
  }

  private List<String> getValuesFileContentsForLocalStore(List<ValuesManifestOutcome> aggregatedValuesManifests) {
    // TODO: implement when local store is available
    return Collections.emptyList();
  }

  private boolean isAnyRemoteStore(@NotEmpty List<ValuesManifestOutcome> aggregatedValuesManifests) {
    return aggregatedValuesManifests.stream().anyMatch(
        valuesManifest -> ManifestStoreType.isInGitSubset(valuesManifest.getStore().getKind()));
  }

  public TaskChainResponse executeNextLink(K8sStepExecutor k8sStepExecutor, Ambiance ambiance,
      K8sStepParameters k8sStepParameters, PassThroughData passThroughData, Map<String, ResponseData> responseDataMap) {
    GitFetchResponse gitFetchResponse = (GitFetchResponse) responseDataMap.values().iterator().next();

    if (gitFetchResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      GitFetchResponsePassThroughData gitFetchResponsePassThroughData =
          GitFetchResponsePassThroughData.builder()
              .errorMsg(gitFetchResponse.getErrorMessage())
              .unitProgressData(gitFetchResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().chainEnd(true).passThroughData(gitFetchResponsePassThroughData).build();
    }
    Map<String, FetchFilesResult> gitFetchFilesResultMap = gitFetchResponse.getFilesFromMultipleRepo();

    K8sStepPassThroughData k8sStepPassThroughData = (K8sStepPassThroughData) passThroughData;

    ManifestOutcome k8sManifest = k8sStepPassThroughData.getK8sManifestOutcome();
    List<ValuesManifestOutcome> valuesManifests = k8sStepPassThroughData.getValuesManifestOutcomes();

    List<String> valuesFileContents = getFileContents(gitFetchFilesResultMap, valuesManifests);

    return k8sStepExecutor.executeK8sTask(
        k8sManifest, ambiance, k8sStepParameters, valuesFileContents, k8sStepPassThroughData.getInfrastructure());
  }

  private List<String> getFileContents(
      Map<String, FetchFilesResult> gitFetchFilesResultMap, List<ValuesManifestOutcome> valuesManifests) {
    List<String> valuesFileContents = new ArrayList<>();

    for (ValuesManifestOutcome valuesManifest : valuesManifests) {
      if (ManifestStoreType.isInGitSubset(valuesManifest.getStore().getKind())) {
        FetchFilesResult gitFetchFilesResult = gitFetchFilesResultMap.get(valuesManifest.getIdentifier());
        valuesFileContents.addAll(
            gitFetchFilesResult.getFiles().stream().map(GitFile::getFileContent).collect(Collectors.toList()));
      }
      // TODO: for local store, add files directly
    }
    return valuesFileContents;
  }

  private HelmCommandFlag getDelegateHelmCommandFlag(List<HelmManifestCommandFlag> commandFlags) {
    if (commandFlags == null) {
      return HelmCommandFlag.builder().valueMap(new HashMap<>()).build();
    }

    Map<HelmSubCommandType, String> commandsValueMap = new HashMap<>();
    for (HelmManifestCommandFlag commandFlag : commandFlags) {
      commandsValueMap.put(commandFlag.getCommandType().getSubCommandType(), commandFlag.getFlag().getValue());
    }

    return HelmCommandFlag.builder().valueMap(commandsValueMap).build();
  }

  public static int getTimeout(K8sStepParameters stepParameters) {
    String timeout = stepParameters.getTimeout() == null || isEmpty(stepParameters.getTimeout().getValue())
        ? YAMLFieldNameConstants.defaultTimeout
        : stepParameters.getTimeout().getValue();

    return NGTimeConversionHelper.convertTimeStringToMinutes(timeout);
  }

  public static String getErrorMessage(K8sDeployResponse k8sDeployResponse) {
    return k8sDeployResponse.getErrorMessage() == null ? "" : k8sDeployResponse.getErrorMessage();
  }

  StepResponse handleGitTaskFailure(GitFetchResponsePassThroughData gitFetchResponse) {
    UnitProgressData unitProgressData = gitFetchResponse.getUnitProgressData();
    return StepResponse.builder()
        .unitProgressList(unitProgressData.getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().setErrorMessage(gitFetchResponse.getErrorMsg()).build())
        .build();
  }

  public static StepResponseBuilder getFailureResponseBuilder(K8sStepParameters k8sStepParameters,
      K8sDeployResponse k8sDeployResponse, StepResponseBuilder stepResponseBuilder) {
    stepResponseBuilder.status(Status.FAILED)
        .failureInfo(
            FailureInfo.newBuilder().setErrorMessage(K8sStepHelper.getErrorMessage(k8sDeployResponse)).build());

    if (k8sStepParameters.getRollbackInfo() != null) {
      stepResponseBuilder.stepOutcome(
          StepResponse.StepOutcome.builder()
              .name("RollbackOutcome")
              .outcome(RollbackOutcome.builder().rollbackInfo(k8sStepParameters.getRollbackInfo()).build())
              .build());
    }

    return stepResponseBuilder;
  }

  public static StepResponseBuilder getDelegateErrorFailureResponseBuilder(
      K8sStepParameters k8sStepParameters, ErrorNotifyResponseData responseData) {
    StepResponseBuilder stepResponseBuilder =
        StepResponse.builder()
            .status(Status.FAILED)
            .failureInfo(FailureInfo.newBuilder().setErrorMessage(responseData.getErrorMessage()).build());

    if (k8sStepParameters.getRollbackInfo() != null) {
      stepResponseBuilder.stepOutcome(
          StepResponse.StepOutcome.builder()
              .name("RollbackOutcome")
              .outcome(RollbackOutcome.builder().rollbackInfo(k8sStepParameters.getRollbackInfo()).build())
              .build());
    }

    return stepResponseBuilder;
  }

  public boolean getSkipResourceVersioning(ManifestOutcome manifestOutcome) {
    switch (manifestOutcome.getType()) {
      case ManifestType.K8Manifest:
        K8sManifestOutcome k8sManifestOutcome = (K8sManifestOutcome) manifestOutcome;
        return k8sManifestOutcome.isSkipResourceVersioning();

      case ManifestType.HelmChart:
        HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;
        return helmChartManifestOutcome.isSkipResourceVersioning();

      case ManifestType.Kustomize:
        KustomizeManifestOutcome kustomizeManifestOutcome = (KustomizeManifestOutcome) manifestOutcome;
        return kustomizeManifestOutcome.isSkipResourceVersioning();

      case ManifestType.OpenshiftTemplate:
        OpenshiftManifestOutcome openshiftManifestOutcome = (OpenshiftManifestOutcome) manifestOutcome;
        return openshiftManifestOutcome.isSkipResourceVersioning();

      default:
        return false;
    }
  }
}

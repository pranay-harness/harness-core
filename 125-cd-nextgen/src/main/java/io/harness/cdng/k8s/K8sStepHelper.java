package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_DIRECT;
import static io.harness.cdng.infra.yaml.InfrastructureKind.KUBERNETES_GCP;
import static io.harness.common.ParameterFieldHelper.getBooleanParameterFieldValue;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.HarnessStringUtils.emptyIfNull;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.k8s.manifest.ManifestHelper.getValuesYamlGitFilePath;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.UnitStatus.RUNNING;
import static io.harness.steps.StepUtils.prepareTaskRequestWithTaskSelector;
import static io.harness.validation.Validator.notEmptyCheck;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sGcpInfrastructureOutcome;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.K8sExecutionPassThroughData;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.mappers.ManifestOutcomeValidator;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome;
import io.harness.cdng.manifest.yaml.HelmChartManifestOutcome.HelmChartManifestOutcomeKeys;
import io.harness.cdng.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.HttpStoreConfig;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome;
import io.harness.cdng.manifest.yaml.K8sManifestOutcome.K8sManifestOutcomeKeys;
import io.harness.cdng.manifest.yaml.KustomizeManifestOutcome;
import io.harness.cdng.manifest.yaml.KustomizeManifestOutcome.KustomizeManifestOutcomeKeys;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftManifestOutcome;
import io.harness.cdng.manifest.yaml.OpenshiftManifestOutcome.OpenshiftManifestOutcomeKeys;
import io.harness.cdng.manifest.yaml.OpenshiftParamManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
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
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.delegate.task.helm.HelmValuesFetchRequest;
import io.harness.delegate.task.helm.HelmValuesFetchResponse;
import io.harness.delegate.task.k8s.DirectK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.GcpK8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sDeployRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sManifestDelegateConfig;
import io.harness.delegate.task.k8s.KustomizeManifestDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.delegate.task.k8s.OpenshiftManifestDelegateConfig;
import io.harness.eraro.Level;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.StepConstants;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.helm.HelmSubCommandType;
import io.harness.k8s.KubernetesHelperService;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.validation.ExpressionUtils;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDP)
@Singleton
public class K8sStepHelper {
  public static final Set<String> K8S_SUPPORTED_MANIFEST_TYPES = ImmutableSet.of(
      ManifestType.K8Manifest, ManifestType.HelmChart, ManifestType.Kustomize, ManifestType.OpenshiftTemplate);

  private static final Set<String> VALUES_YAML_SUPPORTED_MANIFEST_TYPES =
      ImmutableSet.of(ManifestType.K8Manifest, ManifestType.HelmChart);

  public static final String MISSING_INFRASTRUCTURE_ERROR = "Infrastructure section is missing or is not configured";
  public static final String RELEASE_NAME_VALIDATION_REGEX =
      "[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*";
  public static final Pattern releaseNamePattern = Pattern.compile(RELEASE_NAME_VALIDATION_REGEX);
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Named("PRIVILEGED") @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private OutcomeService outcomeService;
  @Inject GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Inject private EncryptionHelper encryptionHelper;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;

  String getReleaseName(InfrastructureOutcome infrastructure) {
    String releaseName;
    switch (infrastructure.getKind()) {
      case KUBERNETES_DIRECT:
        K8sDirectInfrastructureOutcome k8SDirectInfrastructure = (K8sDirectInfrastructureOutcome) infrastructure;
        releaseName = k8SDirectInfrastructure.getReleaseName();
        break;
      case KUBERNETES_GCP:
        K8sGcpInfrastructureOutcome k8sGcpInfrastructure = (K8sGcpInfrastructureOutcome) infrastructure;
        releaseName = k8sGcpInfrastructure.getReleaseName();
        break;
      default:
        throw new UnsupportedOperationException(format("Unknown infrastructure type: [%s]", infrastructure.getKind()));
    }
    validateReleaseName(releaseName);
    return releaseName;
  }

  private static void validateReleaseName(String name) {
    if (!ExpressionUtils.matchesPattern(releaseNamePattern, name)) {
      throw new InvalidRequestException(format(
          "Invalid Release name format: %s. Release name must consist of lower case alphanumeric characters, '-' or '.'"
              + ", and must start and end with an alphanumeric character (e.g. 'example.com')",
          name));
    }
  }

  public ConnectorInfoDTO getConnector(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    IdentifierRef identifierRef = IdentifierRefHelper.getIdentifierRef(
        connectorId, ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(identifierRef.getAccountIdentifier(),
        identifierRef.getOrgIdentifier(), identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(format("Connector not found for identifier : [%s]", connectorId), USER);
    }
    return connectorDTO.get().getConnector();
  }

  public void validateManifest(String manifestStoreType, ConnectorInfoDTO connectorInfoDTO, String message) {
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

      case ManifestStoreType.S3:
        if (!((connectorInfoDTO.getConnectorConfig()) instanceof AwsConnectorDTO)) {
          throw new InvalidRequestException(
              format("Invalid connector selected in %s. Select Amazon Web Services connector", message));
        }
        break;

      case ManifestStoreType.GCS:
        if (!(connectorInfoDTO.getConnectorConfig() instanceof GcpConnectorDTO)) {
          throw new InvalidRequestException(
              format("Invalid connector selected in %s. Select Google cloud connector", message));
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
            .chartName(getParameterFieldValue(helmChartManifestOutcome.getChartName()))
            .chartVersion(getParameterFieldValue(helmChartManifestOutcome.getChartVersion()))
            .helmVersion(helmChartManifestOutcome.getHelmVersion())
            .helmCommandFlag(getDelegateHelmCommandFlag(helmChartManifestOutcome.getCommandFlags()))
            .build();

      case ManifestType.Kustomize:
        KustomizeManifestOutcome kustomizeManifestOutcome = (KustomizeManifestOutcome) manifestOutcome;
        StoreConfig storeConfig = kustomizeManifestOutcome.getStore();
        if (!ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
          throw new UnsupportedOperationException(
              format("Kustomize Manifest is not supported for store type: [%s]", storeConfig.getKind()));
        }
        GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
        return KustomizeManifestDelegateConfig.builder()
            .storeDelegateConfig(getStoreDelegateConfig(kustomizeManifestOutcome.getStore(), ambiance,
                manifestOutcome.getType(), manifestOutcome.getType() + " manifest"))
            .pluginPath(getParameterFieldValue(kustomizeManifestOutcome.getPluginPath()))
            .kustomizeDirPath(getParameterFieldValue(gitStoreConfig.getFolderPath()))
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

      List<String> gitFilePaths = getPathsBasedOnManifest(gitStoreConfig, manifestType);
      return getGitStoreDelegateConfig(
          gitStoreConfig, connectorDTO, encryptedDataDetails, sshKeySpecDTO, gitConfigDTO, manifestType, gitFilePaths);
    }

    if (ManifestStoreType.HTTP.equals(storeConfig.getKind())) {
      HttpStoreConfig httpStoreConfig = (HttpStoreConfig) storeConfig;
      ConnectorInfoDTO helmConnectorDTO =
          getConnector(getParameterFieldValue(httpStoreConfig.getConnectorRef()), ambiance);
      validateManifest(storeConfig.getKind(), helmConnectorDTO, validationErrorMessage);

      return HttpHelmStoreDelegateConfig.builder()
          .repoName(helmConnectorDTO.getIdentifier())
          .repoDisplayName(helmConnectorDTO.getName())
          .httpHelmConnector((HttpHelmConnectorDTO) helmConnectorDTO.getConnectorConfig())
          .encryptedDataDetails(getEncryptionDataDetails(helmConnectorDTO, AmbianceHelper.getNgAccess(ambiance)))
          .build();
    }

    if (ManifestStoreType.S3.equals(storeConfig.getKind())) {
      S3StoreConfig s3StoreConfig = (S3StoreConfig) storeConfig;
      ConnectorInfoDTO awsConnectorDTO =
          getConnector(getParameterFieldValue(s3StoreConfig.getConnectorRef()), ambiance);
      validateManifest(storeConfig.getKind(), awsConnectorDTO, validationErrorMessage);

      return S3HelmStoreDelegateConfig.builder()
          .repoName(awsConnectorDTO.getIdentifier())
          .repoDisplayName(awsConnectorDTO.getName())
          .bucketName(getParameterFieldValue(s3StoreConfig.getBucketName()))
          .region(getParameterFieldValue(s3StoreConfig.getRegion()))
          .folderPath(getParameterFieldValue(s3StoreConfig.getFolderPath()))
          .awsConnector((AwsConnectorDTO) awsConnectorDTO.getConnectorConfig())
          .encryptedDataDetails(getEncryptionDataDetails(awsConnectorDTO, AmbianceHelper.getNgAccess(ambiance)))
          .build();
    }

    if (ManifestStoreType.GCS.equals(storeConfig.getKind())) {
      GcsStoreConfig gcsStoreConfig = (GcsStoreConfig) storeConfig;
      ConnectorInfoDTO gcpConnectorDTO =
          getConnector(getParameterFieldValue(gcsStoreConfig.getConnectorRef()), ambiance);
      validateManifest(storeConfig.getKind(), gcpConnectorDTO, validationErrorMessage);

      return GcsHelmStoreDelegateConfig.builder()
          .repoName(gcpConnectorDTO.getIdentifier())
          .repoDisplayName(gcpConnectorDTO.getName())
          .bucketName(getParameterFieldValue(gcsStoreConfig.getBucketName()))
          .folderPath(getParameterFieldValue(gcsStoreConfig.getFolderPath()))
          .gcpConnector((GcpConnectorDTO) gcpConnectorDTO.getConnectorConfig())
          .encryptedDataDetails(getEncryptionDataDetails(gcpConnectorDTO, AmbianceHelper.getNgAccess(ambiance)))
          .build();
    }

    throw new UnsupportedOperationException(format("Unsupported Store Config type: [%s]", storeConfig.getKind()));
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(@Nonnull GitStoreConfig gitstoreConfig,
      @Nonnull ConnectorInfoDTO connectorDTO, @Nonnull List<EncryptedDataDetail> encryptedDataDetailList,
      SSHKeySpecDTO sshKeySpecDTO, @Nonnull GitConfigDTO gitConfigDTO, String manifestType, List<String> paths) {
    convertToRepoGitConfig(gitstoreConfig, gitConfigDTO);
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO(gitConfigDTO)
        .sshKeySpecDTO(sshKeySpecDTO)
        .encryptedDataDetails(encryptedDataDetailList)
        .fetchType(gitstoreConfig.getGitFetchType())
        .branch(trim(getParameterFieldValue(gitstoreConfig.getBranch())))
        .commitId(trim(getParameterFieldValue(gitstoreConfig.getCommitId())))
        .paths(trimStrings(paths))
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
        paths.add(getParameterFieldValue(gitstoreConfig.getFolderPath()));
        break;
      case ManifestType.Kustomize:
        paths.add("");
        break;

      default:
        paths.addAll(getParameterFieldValue(gitstoreConfig.getPaths()));
    }

    return paths;
  }

  private List<String> getValuesPathsBasedOnManifest(GitStoreConfig gitstoreConfig, String manifestType) {
    List<String> paths = new ArrayList<>();
    switch (manifestType) {
      case ManifestType.HelmChart:
        String folderPath = getParameterFieldValue(gitstoreConfig.getFolderPath());
        paths.add(getValuesYamlGitFilePath(folderPath));
        break;
      case ManifestType.K8Manifest:
        List<String> filePaths = getParameterFieldValue(gitstoreConfig.getPaths());
        for (String filePath : filePaths) {
          paths.add(getValuesYamlGitFilePath(filePath));
        }
        break;
      default:
        throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestType));
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
          return emptyList();
        }

      case HTTP_HELM_REPO:
        HttpHelmConnectorDTO httpHelmConnectorDTO = (HttpHelmConnectorDTO) connectorDTO.getConnectorConfig();
        List<DecryptableEntity> decryptableEntities = httpHelmConnectorDTO.getDecryptableEntities();
        if (isNotEmpty(decryptableEntities)) {
          return secretManagerClientService.getEncryptionDetails(ngAccess, decryptableEntities.get(0));
        } else {
          return emptyList();
        }

      case AWS:
        AwsConnectorDTO awsConnectorDTO = (AwsConnectorDTO) connectorDTO.getConnectorConfig();
        List<DecryptableEntity> awsDecryptableEntities = awsConnectorDTO.getDecryptableEntities();
        if (isNotEmpty(awsDecryptableEntities)) {
          return secretManagerClientService.getEncryptionDetails(ngAccess, awsDecryptableEntities.get(0));
        } else {
          return emptyList();
        }

      case GCP:
        GcpConnectorDTO gcpConnectorDTO = (GcpConnectorDTO) connectorDTO.getConnectorConfig();
        List<DecryptableEntity> gcpDecryptableEntities = gcpConnectorDTO.getDecryptableEntities();
        if (isNotEmpty(gcpDecryptableEntities)) {
          return secretManagerClientService.getEncryptionDetails(ngAccess, gcpDecryptableEntities.get(0));
        } else {
          return emptyList();
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
        KubernetesHelperService.validateNamespace(k8SDirectInfrastructure.getNamespace());

        return DirectK8sInfraDelegateConfig.builder()
            .namespace(k8SDirectInfrastructure.getNamespace())
            .kubernetesClusterConfigDTO((KubernetesClusterConfigDTO) connectorDTO.getConnectorConfig())
            .encryptionDataDetails(getEncryptionDataDetails(connectorDTO, AmbianceHelper.getNgAccess(ambiance)))
            .build();

      case KUBERNETES_GCP:
        K8sGcpInfrastructureOutcome k8sGcpInfrastructure = (K8sGcpInfrastructureOutcome) infrastructure;
        ConnectorInfoDTO gcpConnectorDTO = getConnector(k8sGcpInfrastructure.getConnectorRef(), ambiance);
        KubernetesHelperService.validateNamespace(k8sGcpInfrastructure.getNamespace());
        KubernetesHelperService.validateCluster(k8sGcpInfrastructure.getCluster());

        return GcpK8sInfraDelegateConfig.builder()
            .namespace(k8sGcpInfrastructure.getNamespace())
            .cluster(k8sGcpInfrastructure.getCluster())
            .gcpConnectorDTO((GcpConnectorDTO) gcpConnectorDTO.getConnectorConfig())
            .encryptionDataDetails(getEncryptionDataDetails(gcpConnectorDTO, AmbianceHelper.getNgAccess(ambiance)))
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

  public TaskChainResponse queueK8sTask(StepElementParameters stepElementParameters, K8sDeployRequest k8sDeployRequest,
      Ambiance ambiance, K8sExecutionPassThroughData executionPassThroughData) {
    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {k8sDeployRequest})
                            .taskType(TaskType.K8S_COMMAND_TASK_NG.name())
                            .timeout(getTimeoutInMillis(stepElementParameters))
                            .async(true)
                            .build();

    String taskName = TaskType.K8S_COMMAND_TASK_NG.getDisplayName() + " : " + k8sDeployRequest.getCommandName();
    K8sSpecParameters k8SSpecParameters = (K8sSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest = prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
        k8SSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(k8SSpecParameters.getDelegateSelectors()))));

    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(true)
        .passThroughData(executionPassThroughData)
        .build();
  }

  public List<String> renderValues(
      ManifestOutcome manifestOutcome, Ambiance ambiance, List<String> valuesFileContents) {
    if (isEmpty(valuesFileContents) || ManifestType.Kustomize.equals(manifestOutcome.getType())) {
      return emptyList();
    }

    List<String> renderedValuesFileContents =
        valuesFileContents.stream()
            .map(valuesFileContent -> engineExpressionService.renderExpression(ambiance, valuesFileContent))
            .collect(Collectors.toList());

    if (ManifestType.OpenshiftTemplate.equals(manifestOutcome.getType())) {
      Collections.reverse(renderedValuesFileContents);
    }

    return renderedValuesFileContents;
  }

  public TaskChainResponse executeValuesFetchTask(Ambiance ambiance, StepElementParameters stepElementParameters,
      InfrastructureOutcome infrastructure, ManifestOutcome k8sManifestOutcome,
      List<ValuesManifestOutcome> aggregatedValuesManifests, String helmValuesYamlContent) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        mapValuesManifestToGitFetchFileConfig(aggregatedValuesManifests, ambiance);
    K8sStepPassThroughData k8sStepPassThroughData = K8sStepPassThroughData.builder()
                                                        .k8sManifestOutcome(k8sManifestOutcome)
                                                        .valuesManifestOutcomes(aggregatedValuesManifests)
                                                        .openshiftParamManifestOutcomes(emptyList())
                                                        .infrastructure(infrastructure)
                                                        .helmValuesFileContent(helmValuesYamlContent)
                                                        .build();

    return getGitFetchFileTaskChainResponse(
        ambiance, gitFetchFilesConfigs, stepElementParameters, k8sStepPassThroughData, false);
  }

  public TaskChainResponse prepareOpenshiftParamFetchTask(Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructure,
      ManifestOutcome k8sManifestOutcome, List<OpenshiftParamManifestOutcome> openshiftParamManifests) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs = new ArrayList<>();
    for (OpenshiftParamManifestOutcome openshiftParamManifest : openshiftParamManifests) {
      if (ManifestStoreType.isInGitSubset(openshiftParamManifest.getStore().getKind())) {
        String validationMessage = format("Openshift Param file with Id [%s]", openshiftParamManifest.getIdentifier());
        GitFetchFilesConfig gitFetchFilesConfig =
            getGitFetchFilesConfig(ambiance, openshiftParamManifest.getIdentifier(), openshiftParamManifest.getStore(),
                validationMessage, ManifestType.OpenshiftParam);
        gitFetchFilesConfigs.add(gitFetchFilesConfig);
      }
    }

    K8sStepPassThroughData k8sStepPassThroughData = K8sStepPassThroughData.builder()
                                                        .k8sManifestOutcome(k8sManifestOutcome)
                                                        .valuesManifestOutcomes(emptyList())
                                                        .openshiftParamManifestOutcomes(openshiftParamManifests)
                                                        .infrastructure(infrastructure)
                                                        .build();

    return getGitFetchFileTaskChainResponse(
        ambiance, gitFetchFilesConfigs, stepElementParameters, k8sStepPassThroughData, true);
  }

  public TaskChainResponse prepareValuesFetchTask(K8sStepExecutor k8sStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructure,
      ManifestOutcome k8sManifestOutcome, List<ValuesManifestOutcome> aggregatedValuesManifests) {
    StoreConfig storeConfig = extractStoreConfigFromK8sOrHelmChartManifestOutcome(k8sManifestOutcome);
    if (ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      ValuesManifestOutcome valuesManifestOutcome =
          ValuesManifestOutcome.builder().identifier(k8sManifestOutcome.getIdentifier()).store(storeConfig).build();
      return prepareGitFetchValuesTaskChainResponse(storeConfig, ambiance, stepElementParameters, infrastructure,
          k8sManifestOutcome, valuesManifestOutcome, aggregatedValuesManifests);
    }

    if (ManifestType.HelmChart.equals(k8sManifestOutcome.getType())) {
      return prepareHelmFetchValuesTaskChainResponse(
          ambiance, stepElementParameters, infrastructure, k8sManifestOutcome, aggregatedValuesManifests);
    }

    return k8sStepExecutor.executeK8sTask(k8sManifestOutcome, ambiance, stepElementParameters, emptyList(),
        K8sExecutionPassThroughData.builder().infrastructure(infrastructure).build(), true);
  }

  private TaskChainResponse prepareGitFetchValuesTaskChainResponse(StoreConfig storeConfig, Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructure,
      ManifestOutcome k8sManifestOutcome, ValuesManifestOutcome valuesManifestOutcome,
      List<ValuesManifestOutcome> aggregatedValuesManifests) {
    LinkedList<ValuesManifestOutcome> orderedValuesManifests = new LinkedList<>(aggregatedValuesManifests);
    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        mapValuesManifestToGitFetchFileConfig(aggregatedValuesManifests, ambiance);

    GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
    if (ManifestType.K8Manifest.equals(k8sManifestOutcome.getType()) && hasOnlyOne(gitStoreConfig.getPaths())) {
      gitFetchFilesConfigs.add(mapK8sOrHelmValuesManifestToGitFetchFileConfig(
          valuesManifestOutcome, ambiance, k8sManifestOutcome.getType()));
      orderedValuesManifests.addFirst(valuesManifestOutcome);
    }

    if (ManifestType.HelmChart.equals(k8sManifestOutcome.getType())) {
      gitFetchFilesConfigs.add(mapK8sOrHelmValuesManifestToGitFetchFileConfig(
          valuesManifestOutcome, ambiance, k8sManifestOutcome.getType()));
      orderedValuesManifests.addFirst(valuesManifestOutcome);
    }

    K8sStepPassThroughData k8sStepPassThroughData = K8sStepPassThroughData.builder()
                                                        .k8sManifestOutcome(k8sManifestOutcome)
                                                        .valuesManifestOutcomes(orderedValuesManifests)
                                                        .openshiftParamManifestOutcomes(emptyList())
                                                        .infrastructure(infrastructure)
                                                        .build();

    return getGitFetchFileTaskChainResponse(
        ambiance, gitFetchFilesConfigs, stepElementParameters, k8sStepPassThroughData, true);
  }

  private GitFetchFilesConfig mapK8sOrHelmValuesManifestToGitFetchFileConfig(
      ValuesManifestOutcome valuesManifestOutcome, Ambiance ambiance, String k8sManifestType) {
    String validationMessage = format("Values YAML with Id [%s]", valuesManifestOutcome.getIdentifier());
    return getValuesGitFetchFilesConfig(ambiance, valuesManifestOutcome.getIdentifier(),
        valuesManifestOutcome.getStore(), validationMessage, k8sManifestType);
  }

  private List<GitFetchFilesConfig> mapValuesManifestToGitFetchFileConfig(
      List<ValuesManifestOutcome> aggregatedValuesManifests, Ambiance ambiance) {
    return aggregatedValuesManifests.stream()
        .filter(valuesManifestOutcome -> ManifestStoreType.isInGitSubset(valuesManifestOutcome.getStore().getKind()))
        .map(valuesManifestOutcome
            -> getGitFetchFilesConfig(ambiance, valuesManifestOutcome.getIdentifier(), valuesManifestOutcome.getStore(),
                format("Values YAML with Id [%s]", valuesManifestOutcome.getIdentifier()), ManifestType.VALUES))
        .collect(Collectors.toList());
  }

  private TaskChainResponse prepareHelmFetchValuesTaskChainResponse(Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructure,
      ManifestOutcome k8sManifestOutcome, List<ValuesManifestOutcome> aggregatedValuesManifests) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    HelmChartManifestDelegateConfig helmManifest =
        (HelmChartManifestDelegateConfig) getManifestDelegateConfig(k8sManifestOutcome, ambiance);
    HelmValuesFetchRequest helmValuesFetchRequest = HelmValuesFetchRequest.builder()
                                                        .accountId(accountId)
                                                        .helmChartManifestDelegateConfig(helmManifest)
                                                        .timeout(getTimeoutInMillis(stepElementParameters))
                                                        .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.HELM_VALUES_FETCH_NG.name())
                                  .parameters(new Object[] {helmValuesFetchRequest})
                                  .build();

    String taskName = TaskType.HELM_VALUES_FETCH_NG.getDisplayName();
    K8sSpecParameters k8SSpecParameters = (K8sSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest = prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
        k8SSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(k8SSpecParameters.getDelegateSelectors()))));

    K8sStepPassThroughData k8sStepPassThroughData = K8sStepPassThroughData.builder()
                                                        .k8sManifestOutcome(k8sManifestOutcome)
                                                        .valuesManifestOutcomes(aggregatedValuesManifests)
                                                        .openshiftParamManifestOutcomes(emptyList())
                                                        .infrastructure(infrastructure)
                                                        .build();

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(k8sStepPassThroughData)
        .build();
  }

  private TaskChainResponse getGitFetchFileTaskChainResponse(Ambiance ambiance,
      List<GitFetchFilesConfig> gitFetchFilesConfigs, StepElementParameters stepElementParameters,
      K8sStepPassThroughData k8sStepPassThroughData, boolean shouldOpenLogStream) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    GitFetchRequest gitFetchRequest = GitFetchRequest.builder()
                                          .gitFetchFilesConfigs(gitFetchFilesConfigs)
                                          .shouldOpenLogStream(shouldOpenLogStream)
                                          .accountId(accountId)
                                          .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                                  .parameters(new Object[] {gitFetchRequest})
                                  .build();

    String taskName = TaskType.GIT_FETCH_NEXT_GEN_TASK.getDisplayName();
    K8sSpecParameters k8SSpecParameters = (K8sSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest = prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
        k8SSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(k8SSpecParameters.getDelegateSelectors()))));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(k8sStepPassThroughData)
        .build();
  }

  private boolean hasOnlyOne(ParameterField<List<String>> pathsParameter) {
    List<String> paths = getParameterFieldValue(pathsParameter);
    return isNotEmpty(paths) && paths.size() == 1;
  }

  private StoreConfig extractStoreConfigFromK8sOrHelmChartManifestOutcome(ManifestOutcome manifestOutcome) {
    switch (manifestOutcome.getType()) {
      case ManifestType.K8Manifest:
        K8sManifestOutcome k8sManifestOutcome = (K8sManifestOutcome) manifestOutcome;
        return k8sManifestOutcome.getStore();
      case ManifestType.HelmChart:
        HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;
        return helmChartManifestOutcome.getStore();
      default:
        throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestOutcome.getType()));
    }
  }

  private GitFetchFilesConfig getGitFetchFilesConfig(
      Ambiance ambiance, String identifier, StoreConfig store, String validationMessage, String manifestType) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
    validateManifest(store.getKind(), connectorDTO, validationMessage);

    GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
    NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
    SSHKeySpecDTO sshKeySpecDTO = getSshKeySpecDTO(gitConfigDTO, ambiance);
    List<EncryptedDataDetail> encryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);

    List<String> gitFilePaths = getPathsBasedOnManifest(gitStoreConfig, manifestType);
    GitStoreDelegateConfig gitStoreDelegateConfig = getGitStoreDelegateConfig(
        gitStoreConfig, connectorDTO, encryptedDataDetails, sshKeySpecDTO, gitConfigDTO, manifestType, gitFilePaths);

    return GitFetchFilesConfig.builder()
        .identifier(identifier)
        .manifestType(manifestType)
        .succeedIfFileNotFound(false)
        .gitStoreDelegateConfig(gitStoreDelegateConfig)
        .build();
  }

  private GitFetchFilesConfig getValuesGitFetchFilesConfig(
      Ambiance ambiance, String identifier, StoreConfig store, String validationMessage, String manifestType) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
    validateManifest(store.getKind(), connectorDTO, validationMessage);

    GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
    NGAccess basicNGAccessObject = AmbianceHelper.getNgAccess(ambiance);
    SSHKeySpecDTO sshKeySpecDTO = getSshKeySpecDTO(gitConfigDTO, ambiance);
    List<EncryptedDataDetail> encryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);

    List<String> gitFilePaths = getValuesPathsBasedOnManifest(gitStoreConfig, manifestType);
    GitStoreDelegateConfig gitStoreDelegateConfig = getGitStoreDelegateConfig(
        gitStoreConfig, connectorDTO, encryptedDataDetails, sshKeySpecDTO, gitConfigDTO, manifestType, gitFilePaths);

    return GitFetchFilesConfig.builder()
        .identifier(identifier)
        .manifestType(ManifestType.VALUES)
        .succeedIfFileNotFound(true)
        .gitStoreDelegateConfig(gitStoreDelegateConfig)
        .build();
  }

  public TaskChainResponse startChainLink(
      K8sStepExecutor k8sStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {
    ManifestsOutcome manifestsOutcome = resolveManifestsOutcome(ambiance);
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    ExpressionEvaluatorUtils.updateExpressions(
        manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    manifestsOutcome.values().forEach(value -> ManifestOutcomeValidator.validate(value, false));

    ManifestOutcome k8sManifestOutcome = getK8sSupportedManifestOutcome(new LinkedList<>(manifestsOutcome.values()));
    if (ManifestType.Kustomize.equals(k8sManifestOutcome.getType())) {
      return k8sStepExecutor.executeK8sTask(k8sManifestOutcome, ambiance, stepElementParameters, emptyList(),
          K8sExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build(), true);
    }

    if (VALUES_YAML_SUPPORTED_MANIFEST_TYPES.contains(k8sManifestOutcome.getType())) {
      return prepareK8sOrHelmWithValuesManifests(k8sStepExecutor, new LinkedList<>(manifestsOutcome.values()),
          k8sManifestOutcome, ambiance, stepElementParameters, infrastructureOutcome);
    } else {
      return prepareOcTemplateWithOcParamManifests(k8sStepExecutor, new LinkedList<>(manifestsOutcome.values()),
          k8sManifestOutcome, ambiance, stepElementParameters, infrastructureOutcome);
    }
  }

  private ManifestsOutcome resolveManifestsOutcome(Ambiance ambiance) {
    ManifestsOutcome manifestsOutcome = null;
    try {
      manifestsOutcome = (ManifestsOutcome) outcomeService.resolve(
          ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));
    } catch (Exception exception) {
      throw new InvalidRequestException("Manifests can't be empty", exception);
    }

    if (isEmpty(manifestsOutcome)) {
      throw new InvalidRequestException("Manifests can't be empty");
    }

    return manifestsOutcome;
  }

  private TaskChainResponse prepareOcTemplateWithOcParamManifests(K8sStepExecutor k8sStepExecutor,
      List<ManifestOutcome> manifestOutcomes, ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructureOutcome) {
    List<OpenshiftParamManifestOutcome> openshiftParamManifests = getOpenshiftParamManifests(manifestOutcomes);
    if (isEmpty(openshiftParamManifests)) {
      return k8sStepExecutor.executeK8sTask(k8sManifestOutcome, ambiance, stepElementParameters, emptyList(),
          K8sExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build(), true);
    }
    if (!isAnyOcParamRemoteStore(openshiftParamManifests)) {
      List<String> openshiftParamContentsForLocalStore = emptyList();
      return k8sStepExecutor.executeK8sTask(k8sManifestOutcome, ambiance, stepElementParameters,
          openshiftParamContentsForLocalStore,
          K8sExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build(), true);
    }

    return prepareOpenshiftParamFetchTask(
        ambiance, stepElementParameters, infrastructureOutcome, k8sManifestOutcome, openshiftParamManifests);
  }

  private TaskChainResponse prepareK8sOrHelmWithValuesManifests(K8sStepExecutor k8sStepExecutor,
      List<ManifestOutcome> manifestOutcomes, ManifestOutcome k8sManifestOutcome, Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructureOutcome) {
    List<ValuesManifestOutcome> aggregatedValuesManifests = getAggregatedValuesManifests(manifestOutcomes);

    if (isNotEmpty(aggregatedValuesManifests) && !isAnyRemoteStore(aggregatedValuesManifests)) {
      List<String> valuesFileContentsForLocalStore = getValuesFileContentsForLocalStore(aggregatedValuesManifests);
      return k8sStepExecutor.executeK8sTask(k8sManifestOutcome, ambiance, stepElementParameters,
          valuesFileContentsForLocalStore,
          K8sExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build(), true);
    }

    return prepareValuesFetchTask(k8sStepExecutor, ambiance, stepElementParameters, infrastructureOutcome,
        k8sManifestOutcome, aggregatedValuesManifests);
  }

  @VisibleForTesting
  public ManifestOutcome getK8sSupportedManifestOutcome(@NotEmpty List<ManifestOutcome> manifestOutcomes) {
    List<ManifestOutcome> k8sManifests =
        manifestOutcomes.stream()
            .filter(manifestOutcome -> K8S_SUPPORTED_MANIFEST_TYPES.contains(manifestOutcome.getType()))
            .collect(Collectors.toList());
    if (isEmpty(k8sManifests)) {
      throw new InvalidRequestException(
          "Manifests are mandatory for K8s step. Select one from " + String.join(", ", K8S_SUPPORTED_MANIFEST_TYPES),
          USER);
    }

    if (k8sManifests.size() > 1) {
      throw new InvalidRequestException(
          "There can be only a single manifest. Select one from " + String.join(", ", K8S_SUPPORTED_MANIFEST_TYPES),
          USER);
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

  @VisibleForTesting
  public List<OpenshiftParamManifestOutcome> getOpenshiftParamManifests(
      @NotEmpty List<ManifestOutcome> manifestOutcomeList) {
    List<OpenshiftParamManifestOutcome> openshiftParamManifests = new ArrayList<>();

    List<OpenshiftParamManifestOutcome> serviceParamsManifests =
        manifestOutcomeList.stream()
            .filter(manifestOutcome -> ManifestType.OpenshiftParam.equals(manifestOutcome.getType()))
            .map(manifestOutcome -> (OpenshiftParamManifestOutcome) manifestOutcome)
            .collect(Collectors.toList());

    if (isNotEmpty(serviceParamsManifests)) {
      openshiftParamManifests.addAll(serviceParamsManifests);
    }
    return openshiftParamManifests;
  }

  private List<String> getValuesFileContentsForLocalStore(List<ValuesManifestOutcome> aggregatedValuesManifests) {
    // TODO: implement when local store is available
    return emptyList();
  }

  private boolean isAnyOcParamRemoteStore(@NotEmpty List<OpenshiftParamManifestOutcome> openshiftParamManifests) {
    return openshiftParamManifests.stream().anyMatch(
        openshiftParamManifest -> ManifestStoreType.isInGitSubset(openshiftParamManifest.getStore().getKind()));
  }

  private boolean isAnyRemoteStore(@NotEmpty List<ValuesManifestOutcome> aggregatedValuesManifests) {
    return aggregatedValuesManifests.stream().anyMatch(
        valuesManifest -> ManifestStoreType.isInGitSubset(valuesManifest.getStore().getKind()));
  }

  public TaskChainResponse executeNextLink(K8sStepExecutor k8sStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    K8sStepPassThroughData k8sStepPassThroughData = (K8sStepPassThroughData) passThroughData;
    ManifestOutcome k8sManifest = k8sStepPassThroughData.getK8sManifestOutcome();
    ResponseData responseData = responseDataSupplier.get();
    UnitProgressData unitProgressData = null;

    try {
      if (responseData instanceof GitFetchResponse) {
        unitProgressData = ((GitFetchResponse) responseData).getUnitProgressData();
        return handleGitFetchFilesResponse(
            responseData, k8sStepExecutor, ambiance, stepElementParameters, k8sStepPassThroughData, k8sManifest);
      }

      if (responseData instanceof HelmValuesFetchResponse) {
        unitProgressData = ((HelmValuesFetchResponse) responseData).getUnitProgressData();
        return handleHelmValuesFetchResponse(
            responseData, k8sStepExecutor, ambiance, stepElementParameters, k8sStepPassThroughData, k8sManifest);
      }
    } catch (Exception e) {
      return TaskChainResponse.builder()
          .chainEnd(true)
          .passThroughData(StepExceptionPassThroughData.builder()
                               .errorMessage(ExceptionUtils.getMessage(e))
                               .unitProgressData(completeUnitProgressData(unitProgressData, ambiance, e))
                               .build())
          .build();
    }

    return k8sStepExecutor.executeK8sTask(k8sManifest, ambiance, stepElementParameters, emptyList(),
        K8sExecutionPassThroughData.builder().infrastructure(k8sStepPassThroughData.getInfrastructure()).build(), true);
  }

  private UnitProgressData completeUnitProgressData(
      UnitProgressData currentProgressData, Ambiance ambiance, Exception exception) {
    if (currentProgressData == null) {
      return UnitProgressData.builder().unitProgresses(new ArrayList<>()).build();
    }

    List<UnitProgress> finalUnitProgressList =
        currentProgressData.getUnitProgresses()
            .stream()
            .map(unitProgress -> {
              if (unitProgress.getStatus() == RUNNING) {
                LogCallback logCallback = getLogCallback(unitProgress.getUnitName(), ambiance, false);
                logCallback.saveExecutionLog(ExceptionUtils.getMessage(exception), LogLevel.ERROR, FAILURE);
                return UnitProgress.newBuilder(unitProgress)
                    .setStatus(UnitStatus.FAILURE)
                    .setEndTime(System.currentTimeMillis())
                    .build();
              }

              return unitProgress;
            })
            .collect(Collectors.toList());

    return UnitProgressData.builder().unitProgresses(finalUnitProgressList).build();
  }

  private TaskChainResponse handleGitFetchFilesResponse(ResponseData responseData, K8sStepExecutor k8sStepExecutor,
      Ambiance ambiance, StepElementParameters stepElementParameters, K8sStepPassThroughData k8sStepPassThroughData,
      ManifestOutcome k8sManifest) {
    GitFetchResponse gitFetchResponse = (GitFetchResponse) responseData;
    if (gitFetchResponse.getTaskStatus() != TaskStatus.SUCCESS) {
      GitFetchResponsePassThroughData gitFetchResponsePassThroughData =
          GitFetchResponsePassThroughData.builder()
              .errorMsg(gitFetchResponse.getErrorMessage())
              .unitProgressData(gitFetchResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().chainEnd(true).passThroughData(gitFetchResponsePassThroughData).build();
    }
    Map<String, FetchFilesResult> gitFetchFilesResultMap = gitFetchResponse.getFilesFromMultipleRepo();
    List<String> valuesFileContents = new ArrayList<>();
    String helmValuesYamlContent = k8sStepPassThroughData.getHelmValuesFileContent();
    if (isNotEmpty(helmValuesYamlContent)) {
      valuesFileContents.add(helmValuesYamlContent);
    }

    if (!gitFetchFilesResultMap.isEmpty()) {
      valuesFileContents.addAll(getFileContents(gitFetchFilesResultMap, k8sStepPassThroughData));
    }

    return k8sStepExecutor.executeK8sTask(k8sManifest, ambiance, stepElementParameters, valuesFileContents,
        K8sExecutionPassThroughData.builder()
            .infrastructure(k8sStepPassThroughData.getInfrastructure())
            .lastActiveUnitProgressData(gitFetchResponse.getUnitProgressData())
            .build(),
        false);
  }

  private TaskChainResponse handleHelmValuesFetchResponse(ResponseData responseData, K8sStepExecutor k8sStepExecutor,
      Ambiance ambiance, StepElementParameters stepElementParameters, K8sStepPassThroughData k8sStepPassThroughData,
      ManifestOutcome k8sManifest) {
    HelmValuesFetchResponse helmValuesFetchResponse = (HelmValuesFetchResponse) responseData;
    if (helmValuesFetchResponse.getCommandExecutionStatus() != SUCCESS) {
      HelmValuesFetchResponsePassThroughData helmValuesFetchPassTroughData =
          HelmValuesFetchResponsePassThroughData.builder()
              .errorMsg(helmValuesFetchResponse.getErrorMessage())
              .unitProgressData(helmValuesFetchResponse.getUnitProgressData())
              .build();
      return TaskChainResponse.builder().chainEnd(true).passThroughData(helmValuesFetchPassTroughData).build();
    }

    String valuesFileContent = helmValuesFetchResponse.getValuesFileContent();
    List<ValuesManifestOutcome> aggregatedValuesManifest = k8sStepPassThroughData.getValuesManifestOutcomes();
    if (isNotEmpty(aggregatedValuesManifest)) {
      return executeValuesFetchTask(ambiance, stepElementParameters, k8sStepPassThroughData.getInfrastructure(),
          k8sStepPassThroughData.getK8sManifestOutcome(), aggregatedValuesManifest, valuesFileContent);
    } else {
      List<String> valuesFileContents =
          (isNotEmpty(valuesFileContent)) ? ImmutableList.of(valuesFileContent) : emptyList();
      return k8sStepExecutor.executeK8sTask(k8sManifest, ambiance, stepElementParameters, valuesFileContents,
          K8sExecutionPassThroughData.builder()
              .infrastructure(k8sStepPassThroughData.getInfrastructure())
              .lastActiveUnitProgressData(helmValuesFetchResponse.getUnitProgressData())
              .build(),
          false);
    }
  }

  private List<String> getFileContents(
      Map<String, FetchFilesResult> gitFetchFilesResultMap, K8sStepPassThroughData k8sStepPassThroughData) {
    ManifestOutcome k8sManifest = k8sStepPassThroughData.getK8sManifestOutcome();
    if (ManifestType.OpenshiftTemplate.equals(k8sManifest.getType())) {
      List<? extends ManifestOutcome> openshiftParamManifestOutcomes =
          k8sStepPassThroughData.getOpenshiftParamManifestOutcomes();
      return getManifestFilesContents(gitFetchFilesResultMap, openshiftParamManifestOutcomes);
    } else {
      List<? extends ManifestOutcome> valuesManifests = k8sStepPassThroughData.getValuesManifestOutcomes();
      return getManifestFilesContents(gitFetchFilesResultMap, valuesManifests);
    }
  }

  private List<String> getManifestFilesContents(
      Map<String, FetchFilesResult> gitFetchFilesResultMap, List<? extends ManifestOutcome> valuesManifests) {
    List<String> valuesFileContents = new ArrayList<>();

    for (ManifestOutcome valuesManifest : valuesManifests) {
      StoreConfig store = extractStoreConfigFromManifestOutcome(valuesManifest);
      if (ManifestStoreType.isInGitSubset(store.getKind())) {
        FetchFilesResult gitFetchFilesResult = gitFetchFilesResultMap.get(valuesManifest.getIdentifier());
        if (gitFetchFilesResult != null) {
          valuesFileContents.addAll(
              gitFetchFilesResult.getFiles().stream().map(GitFile::getFileContent).collect(Collectors.toList()));
        }
      }
      // TODO: for local store, add files directly
    }
    return valuesFileContents;
  }

  private StoreConfig extractStoreConfigFromManifestOutcome(ManifestOutcome manifestOutcome) {
    switch (manifestOutcome.getType()) {
      case ManifestType.VALUES:
        ValuesManifestOutcome valuesManifestOutcome = (ValuesManifestOutcome) manifestOutcome;
        return valuesManifestOutcome.getStore();

      case ManifestType.OpenshiftParam:
        OpenshiftParamManifestOutcome openshiftParamManifestOutcome = (OpenshiftParamManifestOutcome) manifestOutcome;
        return openshiftParamManifestOutcome.getStore();

      default:
        throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestOutcome.getType()));
    }
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

  public static int getTimeoutInMin(StepElementParameters stepParameters) {
    String timeout = getTimeoutValue(stepParameters);
    return NGTimeConversionHelper.convertTimeStringToMinutes(timeout);
  }

  public static long getTimeoutInMillis(StepElementParameters stepParameters) {
    String timeout = getTimeoutValue(stepParameters);
    return NGTimeConversionHelper.convertTimeStringToMilliseconds(timeout);
  }

  public static String getTimeoutValue(StepElementParameters stepParameters) {
    return stepParameters.getTimeout() == null || isEmpty(stepParameters.getTimeout().getValue())
        ? StepConstants.defaultTimeout
        : stepParameters.getTimeout().getValue();
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

  StepResponse handleHelmValuesFetchFailure(HelmValuesFetchResponsePassThroughData helmValuesFetchResponse) {
    UnitProgressData unitProgressData = helmValuesFetchResponse.getUnitProgressData();
    return StepResponse.builder()
        .unitProgressList(unitProgressData.getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder().setErrorMessage(helmValuesFetchResponse.getErrorMsg()).build())
        .build();
  }

  public StepResponse handleStepExceptionFailure(StepExceptionPassThroughData stepException) {
    FailureData failureData = FailureData.newBuilder()
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .setLevel(Level.ERROR.name())
                                  .setCode(GENERAL_ERROR.name())
                                  .setMessage(emptyIfNull(stepException.getErrorMessage()))
                                  .build();
    return StepResponse.builder()
        .unitProgressList(stepException.getUnitProgressData().getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .addAllFailureTypes(failureData.getFailureTypesList())
                         .setErrorMessage(failureData.getMessage())
                         .addFailureData(failureData)
                         .build())
        .build();
  }

  public static StepResponseBuilder getFailureResponseBuilder(
      K8sDeployResponse k8sDeployResponse, StepResponseBuilder stepResponseBuilder) {
    stepResponseBuilder.status(Status.FAILED)
        .failureInfo(
            FailureInfo.newBuilder().setErrorMessage(K8sStepHelper.getErrorMessage(k8sDeployResponse)).build());
    return stepResponseBuilder;
  }

  public boolean getSkipResourceVersioning(ManifestOutcome manifestOutcome) {
    switch (manifestOutcome.getType()) {
      case ManifestType.K8Manifest:
        K8sManifestOutcome k8sManifestOutcome = (K8sManifestOutcome) manifestOutcome;
        return getParameterFieldBooleanValue(k8sManifestOutcome.getSkipResourceVersioning(),
            K8sManifestOutcomeKeys.skipResourceVersioning, k8sManifestOutcome);

      case ManifestType.HelmChart:
        HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;
        return getParameterFieldBooleanValue(helmChartManifestOutcome.getSkipResourceVersioning(),
            HelmChartManifestOutcomeKeys.skipResourceVersioning, helmChartManifestOutcome);

      case ManifestType.Kustomize:
        KustomizeManifestOutcome kustomizeManifestOutcome = (KustomizeManifestOutcome) manifestOutcome;
        return getParameterFieldBooleanValue(kustomizeManifestOutcome.getSkipResourceVersioning(),
            KustomizeManifestOutcomeKeys.skipResourceVersioning, kustomizeManifestOutcome);

      case ManifestType.OpenshiftTemplate:
        OpenshiftManifestOutcome openshiftManifestOutcome = (OpenshiftManifestOutcome) manifestOutcome;
        return getParameterFieldBooleanValue(openshiftManifestOutcome.getSkipResourceVersioning(),
            OpenshiftManifestOutcomeKeys.skipResourceVersioning, openshiftManifestOutcome);

      default:
        return false;
    }
  }

  public InfrastructureOutcome getInfrastructureOutcome(Ambiance ambiance) {
    OptionalOutcome optionalOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    if (!optionalOutcome.isFound()) {
      throw new InvalidRequestException(MISSING_INFRASTRUCTURE_ERROR, USER);
    }

    return (InfrastructureOutcome) optionalOutcome.getOutcome();
  }

  public LogCallback getLogCallback(String commandUnitName, Ambiance ambiance, boolean shouldOpenStream) {
    return new NGLogCallback(logStreamingStepClientFactory, ambiance, commandUnitName, shouldOpenStream);
  }

  public StepResponse handleTaskException(
      Ambiance ambiance, K8sExecutionPassThroughData executionPassThroughData, Exception e) {
    UnitProgressData unitProgressData =
        completeUnitProgressData(executionPassThroughData.getLastActiveUnitProgressData(), ambiance, e);
    FailureData failureData = FailureData.newBuilder()
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .setLevel(Level.ERROR.name())
                                  .setCode(GENERAL_ERROR.name())
                                  .setMessage(emptyIfNull(ExceptionUtils.getMessage(e)))
                                  .build();

    return StepResponse.builder()
        .unitProgressList(unitProgressData.getUnitProgresses())
        .status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .addAllFailureTypes(failureData.getFailureTypesList())
                         .setErrorMessage(failureData.getMessage())
                         .addFailureData(failureData)
                         .build())
        .build();
  }

  public static boolean getParameterFieldBooleanValue(
      ParameterField<?> fieldValue, String fieldName, StepElementParameters stepElement) {
    try {
      return getBooleanParameterFieldValue(fieldValue);
    } catch (Exception e) {
      String message = String.format("%s for field %s in %s step with identifier: %s", e.getMessage(), fieldName,
          stepElement.getType(), stepElement.getIdentifier());
      throw new InvalidArgumentsException(message);
    }
  }

  public static boolean getParameterFieldBooleanValue(
      ParameterField<?> fieldValue, String fieldName, ManifestOutcome manifestOutcome) {
    try {
      return getBooleanParameterFieldValue(fieldValue);
    } catch (Exception e) {
      String message = String.format("%s for field %s in %s manifest with identifier: %s", e.getMessage(), fieldName,
          manifestOutcome.getType(), manifestOutcome.getIdentifier());
      throw new InvalidArgumentsException(message);
    }
  }
}

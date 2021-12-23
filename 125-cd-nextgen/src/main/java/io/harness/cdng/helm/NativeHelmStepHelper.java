package io.harness.cdng.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.FeatureName.OPTIMIZED_GIT_FETCH_FILES;
import static io.harness.common.ParameterFieldHelper.getBooleanParameterFieldValue;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
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
import static io.harness.steps.StepUtils.prepareCDTaskRequest;
import static io.harness.validation.Validator.notEmptyCheck;

import static software.wings.beans.appmanifest.ManifestFile.VALUES_YAML_KEY;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.FeatureName;
import io.harness.cdng.AggregatedManifestHelper;
import io.harness.cdng.ReleaseNameHelper;
import io.harness.cdng.expressions.CDExpressionResolveFunctor;
import io.harness.cdng.featureFlag.CDFeatureFlagHelper;
import io.harness.cdng.helm.beans.NativeHelmExecutionPassThroughData;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.K8sEntityHelper;
import io.harness.cdng.k8s.beans.GitFetchResponsePassThroughData;
import io.harness.cdng.k8s.beans.HelmValuesFetchResponsePassThroughData;
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
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.cdng.manifest.yaml.ValuesManifestOutcome;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.helper.GitApiAccessDecryptionHelper;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.helm.HttpHelmConnectorDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.adapter.ScmConnectorMapper;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessType;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.github.GithubUsernameTokenDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabUsernameTokenDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.GcsHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.HttpHelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.S3HelmStoreDelegateConfig;
import io.harness.delegate.beans.storeconfig.StoreDelegateConfig;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.git.GitFetchRequest;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.delegate.task.git.TaskStatus;
import io.harness.delegate.task.helm.HelmCmdExecResponseNG;
import io.harness.delegate.task.helm.HelmCommandFlag;
import io.harness.delegate.task.helm.HelmCommandRequestNG;
import io.harness.delegate.task.helm.HelmValuesFetchRequest;
import io.harness.delegate.task.helm.HelmValuesFetchResponse;
import io.harness.delegate.task.k8s.HelmChartManifestDelegateConfig;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.ManifestDelegateConfig;
import io.harness.encryption.SecretRefData;
import io.harness.eraro.Level;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.executions.steps.StepConstants;
import io.harness.expression.ExpressionEvaluatorUtils;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.helm.HelmSubCommandType;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.logstreaming.LogStreamingStepClientFactory;
import io.harness.logstreaming.NGLogCallback;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.execution.SdkGraphVisualizationDataService;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.EntityReferenceExtractorUtils;
import io.harness.steps.StepHelper;
import io.harness.supplier.ThrowingSupplier;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;
import software.wings.stencils.DefaultValue;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDP)
@Singleton
public class NativeHelmStepHelper {
  public static final Set<String> HELM_SUPPORTED_MANIFEST_TYPES = ImmutableSet.of(ManifestType.HelmChart);

  public static final String RELEASE_NAME = "Release Name";
  public static final String RELEASE_NAME_VALIDATION_REGEX =
      "[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*";
  public static final Pattern releaseNamePattern = Pattern.compile(RELEASE_NAME_VALIDATION_REGEX);
  @Named("PRIVILEGED") @Inject private SecretManagerClientService secretManagerClientService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private OutcomeService outcomeService;
  @Inject GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Inject private LogStreamingStepClientFactory logStreamingStepClientFactory;
  @Inject private StepHelper stepHelper;
  @Inject private EntityReferenceExtractorUtils entityReferenceExtractorUtils;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  @Inject private SdkGraphVisualizationDataService sdkGraphVisualizationDataService;
  @Inject private K8sEntityHelper k8sEntityHelper;
  @Inject private CDFeatureFlagHelper cdFeatureFlagHelper;
  @Inject private ReleaseNameHelper releaseNameHelper;
  @DefaultValue("10") private int steadyStateTimeout; // Minutes
  List<String> valuesFileContents;

  String getReleaseName(Ambiance ambiance, InfrastructureOutcome infrastructure) {
    return releaseNameHelper.getReleaseName(ambiance, infrastructure);
  }

  public ConnectorInfoDTO getConnector(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return k8sEntityHelper.getConnectorInfoDTO(connectorId, ngAccess);
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
    if (ManifestType.HelmChart.equals(manifestOutcome.getType())) {
      HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;
      String chartName = getParameterFieldValue(helmChartManifestOutcome.getChartName());
      return HelmChartManifestDelegateConfig.builder()
          .storeDelegateConfig(getStoreDelegateConfig(
              helmChartManifestOutcome.getStore(), ambiance, manifestOutcome, manifestOutcome.getType() + " manifest"))
          .chartName(chartName)
          .chartVersion(getParameterFieldValue(helmChartManifestOutcome.getChartVersion()))
          .helmVersion(helmChartManifestOutcome.getHelmVersion())
          .helmCommandFlag(getDelegateHelmCommandFlag(helmChartManifestOutcome.getCommandFlags()))
          .build();
    }

    throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestOutcome.getType()));
  }

  public StoreDelegateConfig getStoreDelegateConfig(
      StoreConfig storeConfig, Ambiance ambiance, ManifestOutcome manifestOutcome, String validationErrorMessage) {
    if (ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      GitStoreConfig gitStoreConfig = (GitStoreConfig) storeConfig;
      ConnectorInfoDTO connectorDTO = getConnector(getParameterFieldValue(gitStoreConfig.getConnectorRef()), ambiance);
      validateManifest(storeConfig.getKind(), connectorDTO, validationErrorMessage);

      List<String> gitFilePaths = getPathsBasedOnManifest(gitStoreConfig, manifestOutcome.getType());
      return getGitStoreDelegateConfig(gitStoreConfig, connectorDTO, manifestOutcome, gitFilePaths, ambiance);
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
          .encryptedDataDetails(
              k8sEntityHelper.getEncryptionDataDetails(helmConnectorDTO, AmbianceUtils.getNgAccess(ambiance)))
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
          .encryptedDataDetails(
              k8sEntityHelper.getEncryptionDataDetails(awsConnectorDTO, AmbianceUtils.getNgAccess(ambiance)))
          .useLatestChartMuseumVersion(cdFeatureFlagHelper.isEnabled(
              AmbianceUtils.getAccountId(ambiance), FeatureName.USE_LATEST_CHARTMUSEUM_VERSION))
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
          .encryptedDataDetails(
              k8sEntityHelper.getEncryptionDataDetails(gcpConnectorDTO, AmbianceUtils.getNgAccess(ambiance)))
          .useLatestChartMuseumVersion(cdFeatureFlagHelper.isEnabled(
              AmbianceUtils.getAccountId(ambiance), FeatureName.USE_LATEST_CHARTMUSEUM_VERSION))
          .build();
    }

    throw new UnsupportedOperationException(format("Unsupported Store Config type: [%s]", storeConfig.getKind()));
  }

  public GitStoreDelegateConfig getGitStoreDelegateConfig(@Nonnull GitStoreConfig gitstoreConfig,
      @Nonnull ConnectorInfoDTO connectorDTO, ManifestOutcome manifestOutcome, List<String> paths, Ambiance ambiance) {
    NGAccess basicNGAccessObject = AmbianceUtils.getNgAccess(ambiance);
    ScmConnector scmConnector;
    List<EncryptedDataDetail> apiAuthEncryptedDataDetails = null;
    GitConfigDTO gitConfigDTO = ScmConnectorMapper.toGitConfigDTO((ScmConnector) connectorDTO.getConnectorConfig());
    SSHKeySpecDTO sshKeySpecDTO = getSshKeySpecDTO(gitConfigDTO, ambiance);
    List<EncryptedDataDetail> encryptedDataDetails =
        gitConfigAuthenticationInfoHelper.getEncryptedDataDetails(gitConfigDTO, sshKeySpecDTO, basicNGAccessObject);

    scmConnector = gitConfigDTO;

    boolean optimizedFilesFetch = isOptimizedFilesFetch(connectorDTO, AmbianceUtils.getAccountId(ambiance));

    if (optimizedFilesFetch) {
      scmConnector = (ScmConnector) connectorDTO.getConnectorConfig();
      addApiAuthIfRequired(scmConnector);
      final DecryptableEntity apiAccessDecryptableEntity =
          GitApiAccessDecryptionHelper.getAPIAccessDecryptableEntity(scmConnector);
      apiAuthEncryptedDataDetails =
          secretManagerClientService.getEncryptionDetails(basicNGAccessObject, apiAccessDecryptableEntity);
    }

    convertToRepoGitConfig(gitstoreConfig, scmConnector);
    return GitStoreDelegateConfig.builder()
        .gitConfigDTO(scmConnector)
        .sshKeySpecDTO(sshKeySpecDTO)
        .encryptedDataDetails(encryptedDataDetails)
        .apiAuthEncryptedDataDetails(apiAuthEncryptedDataDetails)
        .fetchType(gitstoreConfig.getGitFetchType())
        .branch(trim(getParameterFieldValue(gitstoreConfig.getBranch())))
        .commitId(trim(getParameterFieldValue(gitstoreConfig.getCommitId())))
        .paths(trimStrings(paths))
        .connectorName(connectorDTO.getName())
        .manifestType(manifestOutcome.getType())
        .manifestId(manifestOutcome.getIdentifier())
        .optimizedFilesFetch(optimizedFilesFetch)
        .build();
  }

  private void addApiAuthIfRequired(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnectorDTO && ((GithubConnectorDTO) scmConnector).getApiAccess() == null
        && isGithubUsernameTokenAuth((GithubConnectorDTO) scmConnector)) {
      GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) scmConnector;
      SecretRefData tokenRef =
          ((GithubUsernameTokenDTO) ((GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials())
                  .getHttpCredentialsSpec())
              .getTokenRef();
      GithubApiAccessDTO apiAccessDTO = GithubApiAccessDTO.builder()
                                            .type(GithubApiAccessType.TOKEN)
                                            .spec(GithubTokenSpecDTO.builder().tokenRef(tokenRef).build())
                                            .build();
      githubConnectorDTO.setApiAccess(apiAccessDTO);
    } else if (scmConnector instanceof GitlabConnectorDTO && ((GitlabConnectorDTO) scmConnector).getApiAccess() == null
        && isGitlabUsernameTokenAuth((GitlabConnectorDTO) scmConnector)) {
      GitlabConnectorDTO gitlabConnectorDTO = (GitlabConnectorDTO) scmConnector;
      SecretRefData tokenRef =
          ((GitlabUsernameTokenDTO) ((GitlabHttpCredentialsDTO) gitlabConnectorDTO.getAuthentication().getCredentials())
                  .getHttpCredentialsSpec())
              .getTokenRef();
      GitlabApiAccessDTO apiAccessDTO = GitlabApiAccessDTO.builder()
                                            .type(GitlabApiAccessType.TOKEN)
                                            .spec(GitlabTokenSpecDTO.builder().tokenRef(tokenRef).build())
                                            .build();
      gitlabConnectorDTO.setApiAccess(apiAccessDTO);
    }
  }

  private boolean isGithubUsernameTokenAuth(GithubConnectorDTO githubConnectorDTO) {
    return githubConnectorDTO.getAuthentication().getCredentials() instanceof GithubHttpCredentialsDTO
        && ((GithubHttpCredentialsDTO) githubConnectorDTO.getAuthentication().getCredentials())
               .getType()
               .equals(GithubHttpAuthenticationType.USERNAME_AND_TOKEN);
  }

  private boolean isGitlabUsernameTokenAuth(GitlabConnectorDTO gitlabConnectorDTO) {
    return gitlabConnectorDTO.getAuthentication().getCredentials() instanceof GitlabHttpCredentialsDTO
        && ((GitlabHttpCredentialsDTO) gitlabConnectorDTO.getAuthentication().getCredentials())
               .getType()
               .equals(GitlabHttpAuthenticationType.USERNAME_AND_TOKEN);
  }

  private boolean isOptimizedFilesFetch(@Nonnull ConnectorInfoDTO connectorDTO, String accountId) {
    return cdFeatureFlagHelper.isEnabled(accountId, OPTIMIZED_GIT_FETCH_FILES)
        && (isGithubTokenAuth((ScmConnector) connectorDTO.getConnectorConfig())
            || isGitlabTokenAuth((ScmConnector) connectorDTO.getConnectorConfig()));
  }

  private void convertToRepoGitConfig(GitStoreConfig gitstoreConfig, ScmConnector scmConnector) {
    String repoName = gitstoreConfig.getRepoName() != null ? gitstoreConfig.getRepoName().getValue() : null;
    if (scmConnector instanceof GitConfigDTO) {
      GitConfigDTO gitConfigDTO = (GitConfigDTO) scmConnector;
      if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT) {
        String repoUrl = getGitRepoUrl(gitConfigDTO, repoName);
        gitConfigDTO.setUrl(repoUrl);
        gitConfigDTO.setGitConnectionType(GitConnectionType.REPO);
      }
    } else if (scmConnector instanceof GithubConnectorDTO) {
      GithubConnectorDTO githubConnectorDTO = (GithubConnectorDTO) scmConnector;
      if (githubConnectorDTO.getConnectionType() == GitConnectionType.ACCOUNT) {
        String repoUrl = getGitRepoUrl(githubConnectorDTO, repoName);
        githubConnectorDTO.setUrl(repoUrl);
        githubConnectorDTO.setConnectionType(GitConnectionType.REPO);
      }
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      GitlabConnectorDTO gitlabConnectorDTO = (GitlabConnectorDTO) scmConnector;
      if (gitlabConnectorDTO.getConnectionType() == GitConnectionType.ACCOUNT) {
        String repoUrl = getGitRepoUrl(gitlabConnectorDTO, repoName);
        gitlabConnectorDTO.setUrl(repoUrl);
        gitlabConnectorDTO.setConnectionType(GitConnectionType.REPO);
      }
    }
  }

  private boolean isGithubTokenAuth(ScmConnector scmConnector) {
    return scmConnector instanceof GithubConnectorDTO
        && (((GithubConnectorDTO) scmConnector).getApiAccess() != null
            || isGithubUsernameTokenAuth((GithubConnectorDTO) scmConnector));
  }

  private boolean isGitlabTokenAuth(ScmConnector scmConnector) {
    return scmConnector instanceof GitlabConnectorDTO
        && (((GitlabConnectorDTO) scmConnector).getApiAccess() != null
            || isGitlabUsernameTokenAuth((GitlabConnectorDTO) scmConnector));
  }

  private String getGitRepoUrl(ScmConnector scmConnector, String repoName) {
    repoName = trimToEmpty(repoName);
    notEmptyCheck("Repo name cannot be empty for Account level git connector", repoName);
    String purgedRepoUrl = scmConnector.getUrl().replaceAll("/*$", "");
    String purgedRepoName = repoName.replaceAll("^/*", "");
    return purgedRepoUrl + "/" + purgedRepoName;
  }

  private List<String> getPathsBasedOnManifest(GitStoreConfig gitstoreConfig, String manifestType) {
    List<String> paths = new ArrayList<>();
    switch (manifestType) {
      case ManifestType.HelmChart:
        paths.add(getParameterFieldValue(gitstoreConfig.getFolderPath()));
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
        paths.add(getValuesYamlGitFilePath(folderPath, VALUES_YAML_KEY));
        break;
      default:
        throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestType));
    }

    return paths;
  }

  private SSHKeySpecDTO getSshKeySpecDTO(GitConfigDTO gitConfigDTO, Ambiance ambiance) {
    return gitConfigAuthenticationInfoHelper.getSSHKey(gitConfigDTO, AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
  }

  public K8sInfraDelegateConfig getK8sInfraDelegateConfig(InfrastructureOutcome infrastructure, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return k8sEntityHelper.getK8sInfraDelegateConfig(infrastructure, ngAccess);
  }

  public List<EncryptedDataDetail> getEncryptedDataDetails(
      @Nonnull GitConfigDTO gitConfigDTO, @Nonnull Ambiance ambiance) {
    return secretManagerClientService.getEncryptionDetails(
        AmbianceUtils.getNgAccess(ambiance), gitConfigDTO.getGitAuth());
  }

  public TaskChainResponse queueNativeHelmTask(StepElementParameters stepElementParameters,
      HelmCommandRequestNG helmCommandRequest, Ambiance ambiance,
      NativeHelmExecutionPassThroughData executionPassThroughData) {
    TaskData taskData = TaskData.builder()
                            .parameters(new Object[] {helmCommandRequest})
                            .taskType(TaskType.HELM_COMMAND_TASK_NG.name())
                            .timeout(getTimeoutInMillis(stepElementParameters))
                            .async(true)
                            .build();

    String taskName = TaskType.HELM_COMMAND_TASK_NG.getDisplayName() + " : " + helmCommandRequest.getCommandName();
    HelmSpecParameters helmSpecParameters = (HelmSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        helmSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(helmSpecParameters.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));
    return TaskChainResponse.builder()
        .taskRequest(taskRequest)
        .chainEnd(true)
        .passThroughData(executionPassThroughData)
        .build();
  }

  public List<String> renderValues(
      ManifestOutcome manifestOutcome, Ambiance ambiance, List<String> valuesFileContents) {
    if (isEmpty(valuesFileContents)) {
      return emptyList();
    }

    return valuesFileContents.stream()
        .map(valuesFileContent -> engineExpressionService.renderExpression(ambiance, valuesFileContent))
        .collect(Collectors.toList());
  }

  public TaskChainResponse executeValuesFetchTask(Ambiance ambiance, StepElementParameters stepElementParameters,
      InfrastructureOutcome infrastructure, ManifestOutcome helmChartManifestOutcome,
      List<ValuesManifestOutcome> aggregatedValuesManifests, String helmValuesYamlContent) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        mapValuesManifestToGitFetchFileConfig(aggregatedValuesManifests, ambiance);
    NativeHelmStepPassThroughData nativeHelmStepPassThroughData =
        NativeHelmStepPassThroughData.builder()
            .helmChartManifestOutcome(helmChartManifestOutcome)
            .valuesManifestOutcomes(aggregatedValuesManifests)
            .infrastructure(infrastructure)
            .helmValuesFileContent(helmValuesYamlContent)
            .build();

    return getGitFetchFileTaskChainResponse(
        ambiance, gitFetchFilesConfigs, stepElementParameters, nativeHelmStepPassThroughData, false);
  }

  public TaskChainResponse prepareValuesFetchTask(Ambiance ambiance, StepElementParameters stepElementParameters,
      InfrastructureOutcome infrastructure, ManifestOutcome helmChartManifestOutcome,
      List<ValuesManifestOutcome> aggregatedValuesManifests) {
    StoreConfig storeConfig = extractStoreConfigFromHelmChartManifestOutcome(helmChartManifestOutcome);
    if (ManifestStoreType.isInGitSubset(storeConfig.getKind())) {
      ValuesManifestOutcome valuesManifestOutcome = ValuesManifestOutcome.builder()
                                                        .identifier(helmChartManifestOutcome.getIdentifier())
                                                        .store(storeConfig)
                                                        .build();
      return prepareGitFetchValuesTaskChainResponse(storeConfig, ambiance, stepElementParameters, infrastructure,
          helmChartManifestOutcome, valuesManifestOutcome, aggregatedValuesManifests);
    }

    return prepareHelmFetchValuesTaskChainResponse(
        ambiance, stepElementParameters, infrastructure, helmChartManifestOutcome, aggregatedValuesManifests);
  }

  private TaskChainResponse prepareGitFetchValuesTaskChainResponse(StoreConfig storeConfig, Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructure,
      ManifestOutcome helmChartManifestOutcome, ValuesManifestOutcome valuesManifestOutcome,
      List<ValuesManifestOutcome> aggregatedValuesManifests) {
    LinkedList<ValuesManifestOutcome> orderedValuesManifests = new LinkedList<>(aggregatedValuesManifests);
    List<GitFetchFilesConfig> gitFetchFilesConfigs =
        mapValuesManifestToGitFetchFileConfig(aggregatedValuesManifests, ambiance);

    gitFetchFilesConfigs.add(
        mapHelmValuesManifestToGitFetchFileConfig(valuesManifestOutcome, ambiance, helmChartManifestOutcome));
    orderedValuesManifests.addFirst(valuesManifestOutcome);

    NativeHelmStepPassThroughData nativeHelmStepPassThroughData =
        NativeHelmStepPassThroughData.builder()
            .helmChartManifestOutcome(helmChartManifestOutcome)
            .valuesManifestOutcomes(orderedValuesManifests)
            .infrastructure(infrastructure)
            .build();

    return getGitFetchFileTaskChainResponse(
        ambiance, gitFetchFilesConfigs, stepElementParameters, nativeHelmStepPassThroughData, true);
  }

  private GitFetchFilesConfig mapHelmValuesManifestToGitFetchFileConfig(
      ValuesManifestOutcome valuesManifestOutcome, Ambiance ambiance, ManifestOutcome helmChartManifestOutcome) {
    String validationMessage = format("Values YAML with Id [%s]", valuesManifestOutcome.getIdentifier());
    return getValuesGitFetchFilesConfig(ambiance, valuesManifestOutcome.getIdentifier(),
        valuesManifestOutcome.getStore(), validationMessage, helmChartManifestOutcome);
  }

  private List<GitFetchFilesConfig> mapValuesManifestToGitFetchFileConfig(
      List<ValuesManifestOutcome> aggregatedValuesManifests, Ambiance ambiance) {
    return aggregatedValuesManifests.stream()
        .filter(valuesManifestOutcome -> ManifestStoreType.isInGitSubset(valuesManifestOutcome.getStore().getKind()))
        .map(valuesManifestOutcome
            -> getGitFetchFilesConfig(ambiance, valuesManifestOutcome.getStore(),
                format("Values YAML with Id [%s]", valuesManifestOutcome.getIdentifier()), valuesManifestOutcome))
        .collect(Collectors.toList());
  }

  private TaskChainResponse prepareHelmFetchValuesTaskChainResponse(Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructure,
      ManifestOutcome helmChartManifestOutcome, List<ValuesManifestOutcome> aggregatedValuesManifests) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    HelmChartManifestDelegateConfig helmManifest =
        (HelmChartManifestDelegateConfig) getManifestDelegateConfig(helmChartManifestOutcome, ambiance);
    HelmValuesFetchRequest helmValuesFetchRequest = HelmValuesFetchRequest.builder()
                                                        .accountId(accountId)
                                                        .helmChartManifestDelegateConfig(helmManifest)
                                                        .timeout(getTimeoutInMillis(stepElementParameters))
                                                        .closeLogStream(!isAnyRemoteStore(aggregatedValuesManifests))
                                                        .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.HELM_VALUES_FETCH_NG.name())
                                  .parameters(new Object[] {helmValuesFetchRequest})
                                  .build();

    String taskName = TaskType.HELM_VALUES_FETCH_NG.getDisplayName();

    HelmSpecParameters helmSpecParameters = (HelmSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        helmSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(helmSpecParameters.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));

    NativeHelmStepPassThroughData nativeHelmStepPassThroughData =
        NativeHelmStepPassThroughData.builder()
            .helmChartManifestOutcome(helmChartManifestOutcome)
            .valuesManifestOutcomes(aggregatedValuesManifests)
            .infrastructure(infrastructure)
            .build();

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(nativeHelmStepPassThroughData)
        .build();
  }

  private TaskChainResponse getGitFetchFileTaskChainResponse(Ambiance ambiance,
      List<GitFetchFilesConfig> gitFetchFilesConfigs, StepElementParameters stepElementParameters,
      NativeHelmStepPassThroughData nativeHelmStepPassThroughData, boolean shouldOpenLogStream) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    GitFetchRequest gitFetchRequest = GitFetchRequest.builder()
                                          .gitFetchFilesConfigs(gitFetchFilesConfigs)
                                          .shouldOpenLogStream(shouldOpenLogStream)
                                          .closeLogStream(true)
                                          .accountId(accountId)
                                          .build();

    final TaskData taskData = TaskData.builder()
                                  .async(true)
                                  .timeout(getTimeoutInMillis(stepElementParameters))
                                  .taskType(TaskType.GIT_FETCH_NEXT_GEN_TASK.name())
                                  .parameters(new Object[] {gitFetchRequest})
                                  .build();

    String taskName = TaskType.GIT_FETCH_NEXT_GEN_TASK.getDisplayName();
    HelmSpecParameters helmSpecParameters = (HelmSpecParameters) stepElementParameters.getSpec();
    final TaskRequest taskRequest = prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        helmSpecParameters.getCommandUnits(), taskName,
        TaskSelectorYaml.toTaskSelector(emptyIfNull(getParameterFieldValue(helmSpecParameters.getDelegateSelectors()))),
        stepHelper.getEnvironmentType(ambiance));

    return TaskChainResponse.builder()
        .chainEnd(false)
        .taskRequest(taskRequest)
        .passThroughData(nativeHelmStepPassThroughData)
        .build();
  }

  private StoreConfig extractStoreConfigFromHelmChartManifestOutcome(ManifestOutcome manifestOutcome) {
    if (manifestOutcome.getType() == ManifestType.HelmChart) {
      HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;
      return helmChartManifestOutcome.getStore();
    }

    throw new UnsupportedOperationException(format("Unsupported Manifest type: [%s]", manifestOutcome.getType()));
  }

  private GitFetchFilesConfig getGitFetchFilesConfig(
      Ambiance ambiance, StoreConfig store, String validationMessage, ManifestOutcome manifestOutcome) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
    validateManifest(store.getKind(), connectorDTO, validationMessage);

    List<String> gitFilePaths = getPathsBasedOnManifest(gitStoreConfig, manifestOutcome.getType());
    GitStoreDelegateConfig gitStoreDelegateConfig =
        getGitStoreDelegateConfig(gitStoreConfig, connectorDTO, manifestOutcome, gitFilePaths, ambiance);

    return GitFetchFilesConfig.builder()
        .identifier(manifestOutcome.getIdentifier())
        .manifestType(manifestOutcome.getType())
        .succeedIfFileNotFound(false)
        .gitStoreDelegateConfig(gitStoreDelegateConfig)
        .build();
  }

  private GitFetchFilesConfig getValuesGitFetchFilesConfig(Ambiance ambiance, String identifier, StoreConfig store,
      String validationMessage, ManifestOutcome k8sManifestOutcome) {
    GitStoreConfig gitStoreConfig = (GitStoreConfig) store;
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    ConnectorInfoDTO connectorDTO = getConnector(connectorId, ambiance);
    validateManifest(store.getKind(), connectorDTO, validationMessage);

    List<String> gitFilePaths = getValuesPathsBasedOnManifest(gitStoreConfig, k8sManifestOutcome.getType());
    GitStoreDelegateConfig gitStoreDelegateConfig =
        getGitStoreDelegateConfig(gitStoreConfig, connectorDTO, k8sManifestOutcome, gitFilePaths, ambiance);

    return GitFetchFilesConfig.builder()
        .identifier(identifier)
        .manifestType(ManifestType.VALUES)
        .succeedIfFileNotFound(true)
        .gitStoreDelegateConfig(gitStoreDelegateConfig)
        .build();
  }

  public TaskChainResponse startChainLink(
      NativeHelmStepExecutor helmStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters) {
    ManifestsOutcome manifestsOutcome = resolveManifestsOutcome(ambiance);
    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
    ExpressionEvaluatorUtils.updateExpressions(
        manifestsOutcome, new CDExpressionResolveFunctor(engineExpressionService, ambiance));
    validateManifestsOutcome(ambiance, manifestsOutcome);

    ManifestOutcome helmChartManifestOutcome = getHelmSupportedManifestOutcome(manifestsOutcome.values());

    return prepareHelmWithValuesManifests(helmStepExecutor, getOrderedManifestOutcome(manifestsOutcome.values()),
        helmChartManifestOutcome, ambiance, stepElementParameters, infrastructureOutcome);
  }

  protected ManifestsOutcome resolveManifestsOutcome(Ambiance ambiance) {
    OptionalOutcome manifestsOutcome = outcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    if (!manifestsOutcome.isFound()) {
      throw new InvalidRequestException("No manifests found.");
    }

    return (ManifestsOutcome) manifestsOutcome.getOutcome();
  }

  private TaskChainResponse prepareHelmWithValuesManifests(NativeHelmStepExecutor nativeHelmStepExecutor,
      List<ManifestOutcome> manifestOutcomes, ManifestOutcome helmChartManifestOutcome, Ambiance ambiance,
      StepElementParameters stepElementParameters, InfrastructureOutcome infrastructureOutcome) {
    List<ValuesManifestOutcome> aggregatedValuesManifests =
        AggregatedManifestHelper.getAggregatedValuesManifests(manifestOutcomes);

    if (isNotEmpty(aggregatedValuesManifests) && !isAnyRemoteStore(aggregatedValuesManifests)) {
      List<String> valuesFileContentsForLocalStore = getValuesFileContentsForLocalStore(aggregatedValuesManifests);
      return nativeHelmStepExecutor.executeHelmTask(helmChartManifestOutcome, ambiance, stepElementParameters,
          valuesFileContentsForLocalStore,
          NativeHelmExecutionPassThroughData.builder().infrastructure(infrastructureOutcome).build(), true, null);
    }

    return prepareValuesFetchTask(
        ambiance, stepElementParameters, infrastructureOutcome, helmChartManifestOutcome, aggregatedValuesManifests);
  }

  @VisibleForTesting
  public ManifestOutcome getHelmSupportedManifestOutcome(@NotEmpty Collection<ManifestOutcome> manifestOutcomes) {
    List<ManifestOutcome> helmManifests =
        manifestOutcomes.stream()
            .filter(manifestOutcome -> HELM_SUPPORTED_MANIFEST_TYPES.contains(manifestOutcome.getType()))
            .collect(Collectors.toList());
    if (isEmpty(helmManifests)) {
      throw new InvalidRequestException(
          "Manifests are mandatory for Helm step. Select one from " + String.join(", ", HELM_SUPPORTED_MANIFEST_TYPES),
          USER);
    }

    if (helmManifests.size() > 1) {
      throw new InvalidRequestException(
          "There can be only a single manifest. Select one from " + String.join(", ", HELM_SUPPORTED_MANIFEST_TYPES),
          USER);
    }
    return helmManifests.get(0);
  }

  private List<String> getValuesFileContentsForLocalStore(List<ValuesManifestOutcome> aggregatedValuesManifests) {
    // TODO: implement when local store is available
    return emptyList();
  }

  private List<ManifestOutcome> getOrderedManifestOutcome(Collection<ManifestOutcome> manifestOutcomes) {
    return manifestOutcomes.stream()
        .sorted(Comparator.comparingInt(ManifestOutcome::getOrder))
        .collect(Collectors.toCollection(LinkedList::new));
  }

  private boolean isAnyRemoteStore(@NotEmpty List<ValuesManifestOutcome> aggregatedValuesManifests) {
    return aggregatedValuesManifests.stream().anyMatch(
        valuesManifest -> ManifestStoreType.isInGitSubset(valuesManifest.getStore().getKind()));
  }

  public TaskChainResponse executeNextLink(NativeHelmStepExecutor nativeHelmStepExecutor, Ambiance ambiance,
      StepElementParameters stepElementParameters, PassThroughData passThroughData,
      ThrowingSupplier<ResponseData> responseDataSupplier) throws Exception {
    NativeHelmStepPassThroughData helmStepPassThroughData = (NativeHelmStepPassThroughData) passThroughData;
    ManifestOutcome helmChartManifest = helmStepPassThroughData.getHelmChartManifestOutcome();
    ResponseData responseData = responseDataSupplier.get();
    UnitProgressData unitProgressData = null;

    try {
      if (responseData instanceof GitFetchResponse) {
        unitProgressData = ((GitFetchResponse) responseData).getUnitProgressData();
        return handleGitFetchFilesResponse(responseData, nativeHelmStepExecutor, ambiance, stepElementParameters,
            helmStepPassThroughData, helmChartManifest);
      }

      if (responseData instanceof HelmValuesFetchResponse) {
        unitProgressData = ((HelmValuesFetchResponse) responseData).getUnitProgressData();
        return handleHelmValuesFetchResponse(responseData, nativeHelmStepExecutor, ambiance, stepElementParameters,
            helmStepPassThroughData, helmChartManifest);
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

    return nativeHelmStepExecutor.executeHelmTask(helmChartManifest, ambiance, stepElementParameters, emptyList(),
        NativeHelmExecutionPassThroughData.builder()
            .infrastructure(helmStepPassThroughData.getInfrastructure())
            .build(),
        true, unitProgressData);
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

  private TaskChainResponse handleGitFetchFilesResponse(ResponseData responseData,
      NativeHelmStepExecutor nativeHelmStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters,
      NativeHelmStepPassThroughData nativeHelmStepPassThroughData, ManifestOutcome helmChartManifest) {
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
    String helmValuesYamlContent = nativeHelmStepPassThroughData.getHelmValuesFileContent();
    if (isNotEmpty(helmValuesYamlContent)) {
      valuesFileContents.add(helmValuesYamlContent);
    }

    if (!gitFetchFilesResultMap.isEmpty()) {
      valuesFileContents.addAll(getFileContents(gitFetchFilesResultMap, nativeHelmStepPassThroughData));
    }

    this.valuesFileContents = valuesFileContents;

    return nativeHelmStepExecutor.executeHelmTask(helmChartManifest, ambiance, stepElementParameters, emptyList(),
        NativeHelmExecutionPassThroughData.builder()
            .infrastructure(nativeHelmStepPassThroughData.getInfrastructure())
            .build(),
        false, gitFetchResponse.getUnitProgressData());
  }

  private TaskChainResponse handleHelmValuesFetchResponse(ResponseData responseData,
      NativeHelmStepExecutor nativeHelmStepExecutor, Ambiance ambiance, StepElementParameters stepElementParameters,
      NativeHelmStepPassThroughData nativeHelmStepPassThroughData, ManifestOutcome helmChartManifest) {
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
    List<ValuesManifestOutcome> aggregatedValuesManifest = nativeHelmStepPassThroughData.getValuesManifestOutcomes();
    if (isNotEmpty(aggregatedValuesManifest)) {
      return executeValuesFetchTask(ambiance, stepElementParameters, nativeHelmStepPassThroughData.getInfrastructure(),
          nativeHelmStepPassThroughData.getHelmChartManifestOutcome(), aggregatedValuesManifest, valuesFileContent);
    } else {
      List<String> valuesFileContents =
          (isNotEmpty(valuesFileContent)) ? ImmutableList.of(valuesFileContent) : emptyList();
      this.valuesFileContents = valuesFileContents;

      return nativeHelmStepExecutor.executeHelmTask(helmChartManifest, ambiance, stepElementParameters, emptyList(),
          NativeHelmExecutionPassThroughData.builder()
              .infrastructure(nativeHelmStepPassThroughData.getInfrastructure())
              .build(),
          false, helmValuesFetchResponse.getUnitProgressData());
    }
  }

  private List<String> getFileContents(Map<String, FetchFilesResult> gitFetchFilesResultMap,
      NativeHelmStepPassThroughData nativeHelmStepPassThroughData) {
    List<? extends ManifestOutcome> valuesManifests = nativeHelmStepPassThroughData.getValuesManifestOutcomes();
    return getManifestFilesContents(gitFetchFilesResultMap, valuesManifests);
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

  public static String getErrorMessage(HelmCmdExecResponseNG helmCmdExecResponseNG) {
    return helmCmdExecResponseNG.getErrorMessage() == null ? "" : helmCmdExecResponseNG.getErrorMessage();
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
      HelmCmdExecResponseNG helmCmdExecResponseNG, StepResponseBuilder stepResponseBuilder) {
    stepResponseBuilder.status(Status.FAILED)
        .failureInfo(FailureInfo.newBuilder()
                         .setErrorMessage(NativeHelmStepHelper.getErrorMessage(helmCmdExecResponseNG))
                         .build());
    return stepResponseBuilder;
  }

  public boolean getSkipResourceVersioning(ManifestOutcome manifestOutcome) {
    switch (manifestOutcome.getType()) {
      case ManifestType.HelmChart:
        HelmChartManifestOutcome helmChartManifestOutcome = (HelmChartManifestOutcome) manifestOutcome;
        return getParameterFieldBooleanValue(helmChartManifestOutcome.getSkipResourceVersioning(),
            HelmChartManifestOutcomeKeys.skipResourceVersioning, helmChartManifestOutcome);

      default:
        return false;
    }
  }

  public LogCallback getLogCallback(String commandUnitName, Ambiance ambiance, boolean shouldOpenStream) {
    return new NGLogCallback(logStreamingStepClientFactory, ambiance, commandUnitName, shouldOpenStream);
  }

  public StepResponse handleTaskException(
      Ambiance ambiance, NativeHelmExecutionPassThroughData executionPassThroughData, Exception e) throws Exception {
    // Trying to figure out if exception is coming from helm task or it is an exception from delegate service.
    // In the second case we need to close log stream and provide unit progress data as part of response
    if (ExceptionUtils.cause(TaskNGDataException.class, e) != null) {
      throw e;
    }
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

  private void validateManifestsOutcome(Ambiance ambiance, ManifestsOutcome manifestsOutcome) {
    Set<EntityDetailProtoDTO> entityDetails = new HashSet<>();
    manifestsOutcome.values().forEach(value -> {
      entityDetails.addAll(entityReferenceExtractorUtils.extractReferredEntities(ambiance, value.getStore()));
      ManifestOutcomeValidator.validate(value, false);
    });

    pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails);
  }

  public static boolean getParameterFieldBooleanValue(
      ParameterField<?> fieldValue, String fieldName, StepElementParameters stepElement) {
    return getParameterFieldBooleanValue(fieldValue, fieldName,
        String.format("%s step with identifier: %s", stepElement.getType(), stepElement.getIdentifier()));
  }

  public static boolean getParameterFieldBooleanValue(
      ParameterField<?> fieldValue, String fieldName, ManifestOutcome manifestOutcome) {
    return getParameterFieldBooleanValue(fieldValue, fieldName,
        String.format("%s manifest with identifier: %s", manifestOutcome.getType(), manifestOutcome.getIdentifier()));
  }

  public static boolean getParameterFieldBooleanValue(
      ParameterField<?> fieldValue, String fieldName, String description) {
    try {
      return getBooleanParameterFieldValue(fieldValue);
    } catch (Exception e) {
      String message = String.format("%s for field %s in %s", e.getMessage(), fieldName, description);
      throw new InvalidArgumentsException(message);
    }
  }

  public void publishReleaseNameStepDetails(Ambiance ambiance, String releaseName) {
    if (isNotEmpty(releaseName)) {
      sdkGraphVisualizationDataService.publishStepDetailInformation(
          ambiance, NativeHelmReleaseDetailsInfo.builder().releaseName(releaseName).build(), RELEASE_NAME);
    }
  }
}

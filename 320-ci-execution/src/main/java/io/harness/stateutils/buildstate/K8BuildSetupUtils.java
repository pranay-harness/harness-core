package io.harness.stateutils.buildstate;

import static io.harness.beans.sweepingoutputs.PodCleanupDetails.CLEANUP_DETAILS;
import static io.harness.common.BuildEnvironmentConstants.DRONE_AWS_REGION;
import static io.harness.common.BuildEnvironmentConstants.DRONE_NETRC_MACHINE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REMOTE_URL;
import static io.harness.common.CICommonPodConstants.STEP_EXEC;
import static io.harness.common.CIExecutionConstants.ACCOUNT_ID_ATTR;
import static io.harness.common.CIExecutionConstants.AWS_CODE_COMMIT_URL_REGEX;
import static io.harness.common.CIExecutionConstants.BUILD_NUMBER_ATTR;
import static io.harness.common.CIExecutionConstants.GIT_URL_SUFFIX;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_LOG_PREFIX_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PIPELINE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_SECRETS_LIST;
import static io.harness.common.CIExecutionConstants.HARNESS_SERVICE_LOG_KEY_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_WORKSPACE;
import static io.harness.common.CIExecutionConstants.LABEL_REGEX;
import static io.harness.common.CIExecutionConstants.LOCALHOST_IP;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.ORG_ID_ATTR;
import static io.harness.common.CIExecutionConstants.PATH_SEPARATOR;
import static io.harness.common.CIExecutionConstants.PIPELINE_EXECUTION_ID_ATTR;
import static io.harness.common.CIExecutionConstants.PIPELINE_ID_ATTR;
import static io.harness.common.CIExecutionConstants.PROJECT_ID_ATTR;
import static io.harness.common.CIExecutionConstants.STAGE_ID_ATTR;
import static io.harness.common.CIExecutionConstants.TI_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.TI_SERVICE_TOKEN_VARIABLE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.BITBUCKET;
import static io.harness.delegate.beans.connector.ConnectorType.CODECOMMIT;
import static io.harness.delegate.beans.connector.ConnectorType.DOCKER;
import static io.harness.delegate.beans.connector.ConnectorType.GITHUB;
import static io.harness.delegate.beans.connector.ConnectorType.GITLAB;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.trimLeadingCharacter;
import static org.springframework.util.StringUtils.trimTrailingCharacter;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo.ConnectorConversionInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.PodCleanupDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.delegate.beans.ci.CIK8BuildTaskParams;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerSecrets;
import io.harness.delegate.beans.ci.pod.HostAliasParams;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.git.GitClientHelper;
import io.harness.k8s.model.ImageDetails;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.product.ci.engine.proto.Execution;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import io.harness.steps.StepOutcomeGroup;
import io.harness.tiserviceclient.TIServiceUtils;
import io.harness.util.LiteEngineSecretEvaluator;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class K8BuildSetupUtils {
  @Inject private SecretUtils secretUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ServiceTokenUtils serviceTokenUtils;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private InternalContainerParamsProvider internalContainerParamsProvider;
  @Inject private ExecutionProtobufSerializer protobufSerializer;
  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;
  @Inject CILogServiceUtils logServiceUtils;
  @Inject TIServiceUtils tiServiceUtils;

  public CIK8BuildTaskParams getCIk8BuildTaskParams(LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance,
      Map<String, String> taskIds, String logPrefix, Map<String, String> stepLogKeys) {
    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.podDetails));

    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    Infrastructure infrastructure = liteEngineTaskStepInfo.getInfrastructure();

    if (infrastructure == null || ((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    K8sDirectInfraYaml k8sDirectInfraYaml = (K8sDirectInfraYaml) infrastructure;

    final String clusterName = k8sDirectInfraYaml.getSpec().getConnectorRef();

    PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());

    ConnectorDetails k8sConnector = connectorUtils.getConnectorDetails(ngAccess, clusterName);
    String workDir = ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getWorkDir();
    CIK8PodParams<CIK8ContainerParams> podParams = getPodParams(ngAccess, k8PodDetails, liteEngineTaskStepInfo,
        liteEngineTaskStepInfo.isUsePVC(), liteEngineTaskStepInfo.getCiCodebase(),
        liteEngineTaskStepInfo.isSkipGitClone(), workDir, taskIds, logPrefix, stepLogKeys, ambiance);

    log.info("Created pod params for pod name [{}]", podSetupInfo.getName());
    return CIK8BuildTaskParams.builder().k8sConnector(k8sConnector).cik8PodParams(podParams).build();
  }

  public List<ContainerDefinitionInfo> getCIk8BuildServiceContainers(LiteEngineTaskStepInfo liteEngineTaskStepInfo) {
    PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());
    return podSetupInfo.getPodSetupParams()
        .getContainerDefinitionInfos()
        .stream()
        .filter(containerDefinitionInfo -> containerDefinitionInfo.getContainerType().equals(CIContainerType.SERVICE))
        .collect(toList());
  }

  public CIK8PodParams<CIK8ContainerParams> getPodParams(NGAccess ngAccess, K8PodDetails k8PodDetails,
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, boolean usePVC, CodeBase ciCodebase, boolean skipGitClone,
      String workDir, Map<String, String> taskIds, String logPrefix, Map<String, String> stepLogKeys,
      Ambiance ambiance) {
    PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());
    ConnectorDetails harnessInternalImageRegistryConnectorDetails =
        connectorUtils.getConnectorDetails(ngAccess, ciExecutionServiceConfig.getDefaultInternalImageConnector());
    ConnectorDetails gitConnector = getGitConnector(ngAccess, ciCodebase, skipGitClone);
    Map<String, String> gitEnvVars = getGitEnvVariables(gitConnector, ciCodebase);

    List<CIK8ContainerParams> containerParamsList =
        getContainerParamsList(k8PodDetails, podSetupInfo, ngAccess, harnessInternalImageRegistryConnectorDetails,
            gitEnvVars, liteEngineTaskStepInfo, taskIds, logPrefix, stepLogKeys, ambiance);

    CIK8ContainerParams setupAddOnContainerParams =
        internalContainerParamsProvider.getSetupAddonContainerParams(harnessInternalImageRegistryConnectorDetails,
            podSetupInfo.getVolumeToMountPath(), podSetupInfo.getWorkDirPath());

    List<HostAliasParams> hostAliasParamsList = new ArrayList<>();
    if (podSetupInfo.getServiceIdList() != null) {
      hostAliasParamsList.add(
          HostAliasParams.builder().ipAddress(LOCALHOST_IP).hostnameList(podSetupInfo.getServiceIdList()).build());
    }

    List<PVCParams> pvcParamsList = new ArrayList<>();
    if (usePVC) {
      pvcParamsList = podSetupInfo.getPvcParamsList();
    }

    Infrastructure infrastructure = liteEngineTaskStepInfo.getInfrastructure();

    if (infrastructure == null || ((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }
    K8sDirectInfraYaml k8sDirectInfraYaml = (K8sDirectInfraYaml) infrastructure;

    List<String> containerNames =
        containerParamsList.stream().map(CIK8ContainerParams::getName).collect(Collectors.toList());
    containerNames.add(setupAddOnContainerParams.getName());
    executionSweepingOutputResolver.consume(ambiance, CLEANUP_DETAILS,
        PodCleanupDetails.builder()
            .infrastructure(infrastructure)
            .podName(podSetupInfo.getName())
            .cleanUpContainerNames(containerNames)
            .build(),
        StepOutcomeGroup.STAGE.name());

    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(k8sDirectInfraYaml.getSpec().getNamespace())
        .labels(getBuildLabels(ambiance, k8PodDetails))
        .gitConnector(gitConnector)
        .stepExecVolumeName(STEP_EXEC)
        .stepExecWorkingDir(workDir)
        .containerParamsList(containerParamsList)
        .pvcParamList(pvcParamsList)
        .initContainerParamsList(singletonList(setupAddOnContainerParams))
        .hostAliasParamsList(hostAliasParamsList)
        .build();
  }

  public List<CIK8ContainerParams> getContainerParamsList(K8PodDetails k8PodDetails, PodSetupInfo podSetupInfo,
      NGAccess ngAccess, ConnectorDetails harnessInternalImageRegistryConnectorDetails, Map<String, String> gitEnvVars,
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Map<String, String> taskIds, String logPrefix,
      Map<String, String> stepLogKeys, Ambiance ambiance) {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    Map<String, String> logEnvVars = getLogServiceEnvVariables(k8PodDetails, accountId);
    Map<String, String> tiEnvVars = getTIServiceEnvVariables(accountId);
    Map<String, String> commonEnvVars = getCommonStepEnvVariables(
        k8PodDetails, logEnvVars, tiEnvVars, gitEnvVars, podSetupInfo.getWorkDirPath(), logPrefix, ambiance);
    Map<String, ConnectorConversionInfo> stepConnectors =
        ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getStepConnectorRefs();
    Set<String> publishArtifactStepIds =
        ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPublishArtifactStepIds();

    LiteEngineSecretEvaluator liteEngineSecretEvaluator =
        LiteEngineSecretEvaluator.builder().secretUtils(secretUtils).build();
    List<SecretVariableDetails> secretVariableDetails =
        liteEngineSecretEvaluator.resolve(liteEngineTaskStepInfo, ngAccess, ambiance.getExpressionFunctorToken());
    CIK8ContainerParams liteEngineContainerParams = createLiteEngineContainerParams(ngAccess,
        harnessInternalImageRegistryConnectorDetails, stepConnectors, publishArtifactStepIds, liteEngineTaskStepInfo,
        k8PodDetails, podSetupInfo.getStageCpuRequest(), podSetupInfo.getStageMemoryRequest(),
        podSetupInfo.getServiceGrpcPortList(), logEnvVars, tiEnvVars, podSetupInfo.getVolumeToMountPath(),
        podSetupInfo.getWorkDirPath(), taskIds, logPrefix, stepLogKeys, ambiance);

    List<CIK8ContainerParams> containerParams = new ArrayList<>();
    containerParams.add(liteEngineContainerParams);
    // user input containers with custom entry point
    for (ContainerDefinitionInfo containerDefinitionInfo :
        podSetupInfo.getPodSetupParams().getContainerDefinitionInfos()) {
      CIK8ContainerParams cik8ContainerParams =
          createCIK8ContainerParams(ngAccess, containerDefinitionInfo, commonEnvVars, stepConnectors,
              podSetupInfo.getVolumeToMountPath(), podSetupInfo.getWorkDirPath(), logPrefix, secretVariableDetails);
      containerParams.add(cik8ContainerParams);
    }
    return containerParams;
  }

  private CIK8ContainerParams createCIK8ContainerParams(NGAccess ngAccess,
      ContainerDefinitionInfo containerDefinitionInfo, Map<String, String> commonEnvVars,
      Map<String, ConnectorConversionInfo> connectorRefs, Map<String, String> volumeToMountPath, String workDirPath,
      String logPrefix, List<SecretVariableDetails> secretVariableDetails) {
    Map<String, String> envVars = new HashMap<>(commonEnvVars);
    if (isNotEmpty(containerDefinitionInfo.getEnvVars())) {
      envVars.putAll(containerDefinitionInfo.getEnvVars()); // Put customer input env variables
    }
    Map<String, ConnectorDetails> stepConnectorDetails = emptyMap();
    if (isNotEmpty(containerDefinitionInfo.getStepIdentifier()) && isNotEmpty(connectorRefs)) {
      ConnectorConversionInfo connectorConversionInfo = connectorRefs.get(containerDefinitionInfo.getStepIdentifier());
      if (connectorConversionInfo != null) {
        ConnectorDetails connectorDetails =
            connectorUtils.getConnectorDetailsWithConversionInfo(ngAccess, connectorConversionInfo);
        stepConnectorDetails = singletonMap(connectorDetails.getIdentifier(), connectorDetails);
      }
    }

    ImageDetails imageDetails = containerDefinitionInfo.getContainerImageDetails().getImageDetails();
    ConnectorDetails connectorDetails = null;
    if (containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier() != null) {
      connectorDetails = connectorUtils.getConnectorDetails(
          ngAccess, containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier());
      String fullyQualifiedImageName = getImageName(connectorDetails, imageDetails.getName());
      imageDetails.setName(fullyQualifiedImageName);
    }
    ImageDetailsWithConnector imageDetailsWithConnector =
        ImageDetailsWithConnector.builder().imageConnectorDetails(connectorDetails).imageDetails(imageDetails).build();

    List<SecretVariableDetails> containerSecretVariableDetails =
        getSecretVariableDetails(ngAccess, containerDefinitionInfo, secretVariableDetails);

    envVars.putAll(createEnvVariableForSecret(containerSecretVariableDetails));
    if (containerDefinitionInfo.getContainerType() == CIContainerType.SERVICE) {
      envVars.put(HARNESS_SERVICE_LOG_KEY_VARIABLE,
          format("%s/serviceId:%s", logPrefix, containerDefinitionInfo.getStepIdentifier()));
    }
    return CIK8ContainerParams.builder()
        .name(containerDefinitionInfo.getName())
        .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
        .containerType(containerDefinitionInfo.getContainerType())
        .envVars(envVars)
        .containerSecrets(ContainerSecrets.builder()
                              .secretVariableDetails(containerSecretVariableDetails)
                              .connectorDetailsMap(stepConnectorDetails)
                              .build())
        .commands(containerDefinitionInfo.getCommands())
        .ports(containerDefinitionInfo.getPorts())
        .args(containerDefinitionInfo.getArgs())
        .imageDetailsWithConnector(imageDetailsWithConnector)
        .volumeToMountPath(volumeToMountPath)
        .workingDir(workDirPath)
        .build();
  }

  private Map<String, String> createEnvVariableForSecret(List<SecretVariableDetails> secretVariableDetails) {
    Map<String, String> envVars = new HashMap<>();

    if (isNotEmpty(secretVariableDetails)) {
      List<String> secretEnvNames =
          secretVariableDetails.stream()
              .map(secretVariableDetail -> { return secretVariableDetail.getSecretVariableDTO().getName(); })
              .collect(Collectors.toList());
      envVars.put(HARNESS_SECRETS_LIST, String.join(",", secretEnvNames));
    }
    return envVars;
  }

  private CIK8ContainerParams createLiteEngineContainerParams(NGAccess ngAccess, ConnectorDetails connectorDetails,
      Map<String, ConnectorConversionInfo> connectorRefs, Set<String> publishArtifactStepIds,
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, K8PodDetails k8PodDetails, Integer stageCpuRequest,
      Integer stageMemoryRequest, List<Integer> serviceGrpcPortList, Map<String, String> logEnvVars,
      Map<String, String> tiEnvVars, Map<String, String> volumeToMountPath, String workDirPath,
      Map<String, String> taskIds, String logPrefix, Map<String, String> stepLogKeys, Ambiance ambiance) {
    Map<String, ConnectorDetails> stepConnectorDetails = new HashMap<>();
    if (isNotEmpty(publishArtifactStepIds)) {
      for (String publishArtifactStepId : publishArtifactStepIds) {
        ConnectorDetails publishArtifactConnector =
            connectorUtils.getConnectorDetailsWithConversionInfo(ngAccess, connectorRefs.get(publishArtifactStepId));
        stepConnectorDetails.put(publishArtifactConnector.getIdentifier(), publishArtifactConnector);
      }
    }
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String serializedLiteEngineStepInfo =
        getSerializedLiteEngineStepInfo(liteEngineTaskStepInfo, taskIds, accountId, stepLogKeys);
    String serviceToken = serviceTokenUtils.getServiceToken();
    return internalContainerParamsProvider.getLiteEngineContainerParams(connectorDetails, stepConnectorDetails,
        k8PodDetails, serializedLiteEngineStepInfo, serviceToken, stageCpuRequest, stageMemoryRequest,
        serviceGrpcPortList, logEnvVars, tiEnvVars, volumeToMountPath, workDirPath, logPrefix, ambiance);
  }

  private String getSerializedLiteEngineStepInfo(LiteEngineTaskStepInfo liteEngineTaskStepInfo,
      Map<String, String> taskIds, String accountId, Map<String, String> stepLogKeys) {
    Execution executionPrototype = protobufSerializer.convertExecutionElement(
        liteEngineTaskStepInfo.getExecutionElementConfig(), liteEngineTaskStepInfo, taskIds, stepLogKeys);
    Execution execution = Execution.newBuilder(executionPrototype).setAccountId(accountId).build();
    return Base64.encodeBase64String(execution.toByteArray());
  }

  @NotNull
  private PodSetupInfo getPodSetupInfo(K8BuildJobEnvInfo k8BuildJobEnvInfo) {
    // Supporting single pod currently
    Optional<PodSetupInfo> podSetupInfoOpt =
        k8BuildJobEnvInfo.getPodsSetupInfo().getPodSetupInfoList().stream().findFirst();
    if (!podSetupInfoOpt.isPresent()) {
      throw new InvalidRequestException("Pod setup info can not be empty");
    }
    return podSetupInfoOpt.get();
  }

  @NotNull
  private List<SecretVariableDetails> getSecretVariableDetails(NGAccess ngAccess,
      ContainerDefinitionInfo containerDefinitionInfo, List<SecretVariableDetails> scriptsSecretVariableDetails) {
    List<SecretVariableDetails> secretVariableDetails = new ArrayList<>();
    secretVariableDetails.addAll(scriptsSecretVariableDetails);
    if (isNotEmpty(containerDefinitionInfo.getSecretVariables())) {
      containerDefinitionInfo.getSecretVariables().forEach(
          secretVariable -> secretVariableDetails.add(secretUtils.getSecretVariableDetails(ngAccess, secretVariable)));
    }
    return secretVariableDetails.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  @NotNull
  private Map<String, String> getLogServiceEnvVariables(K8PodDetails k8PodDetails, String accountID) {
    Map<String, String> envVars = new HashMap<>();
    final String logServiceBaseUrl = logServiceUtils.getLogServiceConfig().getBaseUrl();

    // Make a call to the log service and get back the token
    String logServiceToken = logServiceUtils.getLogServiceToken(accountID);
    envVars.put(LOG_SERVICE_TOKEN_VARIABLE, logServiceToken);
    envVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, logServiceBaseUrl);

    return envVars;
  }

  @NotNull
  private Map<String, String> getTIServiceEnvVariables(String accountId) {
    Map<String, String> envVars = new HashMap<>();
    final String tiServiceBaseUrl = tiServiceUtils.getTiServiceConfig().getBaseUrl();

    String tiServiceToken = "token";

    // Make a call to the TI service and get back the token. We do not need TI service token for all steps,
    // so we can continue even if the service is down.
    // TODO: (vistaar) Get token only when TI service interaction is required.
    try {
      tiServiceToken = tiServiceUtils.getTIServiceToken(accountId);
    } catch (Exception e) {
      log.error("Could not call token endpoint for TI service", e);
    }

    envVars.put(TI_SERVICE_TOKEN_VARIABLE, tiServiceToken);
    envVars.put(TI_SERVICE_ENDPOINT_VARIABLE, tiServiceBaseUrl);

    return envVars;
  }

  @NotNull
  private Map<String, String> getCommonStepEnvVariables(K8PodDetails k8PodDetails, Map<String, String> logEnvVars,
      Map<String, String> tiEnvVars, Map<String, String> gitEnvVars, String workDirPath, String logPrefix,
      Ambiance ambiance) {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = AmbianceHelper.getAccountId(ambiance);
    final String orgID = AmbianceHelper.getOrgIdentifier(ambiance);
    final String projectID = AmbianceHelper.getProjectIdentifier(ambiance);
    final String pipelineID = ambiance.getMetadata().getPipelineIdentifier();
    final int buildNumber = ambiance.getMetadata().getRunSequence();
    final String stageID = k8PodDetails.getStageID();

    // Add log service environment variables
    envVars.putAll(logEnvVars);
    // Add TI service environment variables
    envVars.putAll(tiEnvVars);
    // Add git connector environment variables
    envVars.putAll(gitEnvVars);

    // Add other environment variables needed in the containers
    envVars.put(HARNESS_WORKSPACE, workDirPath);
    envVars.put(HARNESS_ACCOUNT_ID_VARIABLE, accountID);
    envVars.put(HARNESS_PROJECT_ID_VARIABLE, projectID);
    envVars.put(HARNESS_ORG_ID_VARIABLE, orgID);
    envVars.put(HARNESS_PIPELINE_ID_VARIABLE, pipelineID);
    envVars.put(HARNESS_BUILD_ID_VARIABLE, String.valueOf(buildNumber));
    envVars.put(HARNESS_STAGE_ID_VARIABLE, stageID);
    envVars.put(HARNESS_LOG_PREFIX_VARIABLE, logPrefix);
    return envVars;
  }

  @VisibleForTesting
  Map<String, String> getGitEnvVariables(ConnectorDetails gitConnector, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    if (gitConnector == null) {
      return envVars;
    }

    validateGitConnector(gitConnector);
    if (gitConnector.getConnectorType() == GITHUB) {
      GithubConnectorDTO gitConfigDTO = (GithubConnectorDTO) gitConnector.getConnectorConfig();
      envVars = retrieveGithubEnvVar(gitConfigDTO, ciCodebase);
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnectorDTO gitConfigDTO = (GitlabConnectorDTO) gitConnector.getConnectorConfig();
      envVars = retrieveGitlabEnvVar(gitConfigDTO, ciCodebase);
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnectorDTO gitConfigDTO = (BitbucketConnectorDTO) gitConnector.getConnectorConfig();
      envVars = retrieveBitbucketEnvVar(gitConfigDTO, ciCodebase);
    } else if (gitConnector.getConnectorType() == CODECOMMIT) {
      AwsCodeCommitConnectorDTO gitConfigDTO = (AwsCodeCommitConnectorDTO) gitConnector.getConnectorConfig();
      envVars = retrieveAwsCodeCommitEnvVar(gitConfigDTO, ciCodebase);
    } else {
      throw new CIStageExecutionException("Unsupported git connector type" + gitConnector.getConnectorType());
    }

    return envVars;
  }

  private Map<String, String> retrieveGithubEnvVar(GithubConnectorDTO gitConfigDTO, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    String gitUrl = getGitURL(ciCodebase, gitConfigDTO.getConnectionType(), gitConfigDTO.getUrl());
    String domain = GitClientHelper.getGitSCM(gitUrl);

    envVars.put(DRONE_REMOTE_URL, gitUrl);
    envVars.put(DRONE_NETRC_MACHINE, domain);
    switch (gitConfigDTO.getAuthentication().getAuthType()) {
      case HTTP:
        GithubHttpCredentialsDTO gitAuth = (GithubHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();
        if (gitAuth.getType() != GithubHttpAuthenticationType.USERNAME_AND_PASSWORD
            && gitAuth.getType() != GithubHttpAuthenticationType.USERNAME_AND_TOKEN) {
          throw new CIStageExecutionException("Unsupported github connector auth type" + gitAuth.getType());
        }
        break;
      case SSH:
        break;
      default:
        throw new CIStageExecutionException(
            "Unsupported github connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }
    return envVars;
  }

  private Map<String, String> retrieveGitlabEnvVar(GitlabConnectorDTO gitConfigDTO, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    String gitUrl = getGitURL(ciCodebase, gitConfigDTO.getConnectionType(), gitConfigDTO.getUrl());
    String domain = GitClientHelper.getGitSCM(gitUrl);

    envVars.put(DRONE_REMOTE_URL, gitUrl);
    envVars.put(DRONE_NETRC_MACHINE, domain);
    switch (gitConfigDTO.getAuthentication().getAuthType()) {
      case HTTP:
        GitlabHttpCredentialsDTO gitAuth = (GitlabHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();
        if (gitAuth.getType() != GitlabHttpAuthenticationType.USERNAME_AND_PASSWORD
            && gitAuth.getType() != GitlabHttpAuthenticationType.USERNAME_AND_TOKEN) {
          throw new CIStageExecutionException("Unsupported gitlab connector auth type" + gitAuth.getType());
        }
        break;
      case SSH:
        break;
      default:
        throw new CIStageExecutionException(
            "Unsupported gitlab connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }
    return envVars;
  }

  private Map<String, String> retrieveBitbucketEnvVar(BitbucketConnectorDTO gitConfigDTO, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    String gitUrl = getGitURL(ciCodebase, gitConfigDTO.getConnectionType(), gitConfigDTO.getUrl());
    String domain = GitClientHelper.getGitSCM(gitUrl);

    envVars.put(DRONE_REMOTE_URL, gitUrl);
    envVars.put(DRONE_NETRC_MACHINE, domain);
    switch (gitConfigDTO.getAuthentication().getAuthType()) {
      case HTTP:
        BitbucketHttpCredentialsDTO gitAuth =
            (BitbucketHttpCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();
        if (gitAuth.getType() != BitbucketHttpAuthenticationType.USERNAME_AND_PASSWORD) {
          throw new CIStageExecutionException("Unsupported bitbucket connector auth type" + gitAuth.getType());
        }
        break;
      case SSH:
        break;
      default:
        throw new CIStageExecutionException(
            "Unsupported bitbucket connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }
    return envVars;
  }

  private Map<String, String> retrieveAwsCodeCommitEnvVar(AwsCodeCommitConnectorDTO gitConfigDTO, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    GitConnectionType gitConnectionType =
        gitConfigDTO.getUrlType() == AwsCodeCommitUrlType.REPO ? GitConnectionType.REPO : GitConnectionType.ACCOUNT;
    String gitUrl = getGitURL(ciCodebase, gitConnectionType, gitConfigDTO.getUrl());

    envVars.put(DRONE_REMOTE_URL, gitUrl);
    envVars.put(DRONE_AWS_REGION, getAwsCodeCommitRegion(gitConfigDTO.getUrl()));
    if (gitConfigDTO.getAuthentication().getAuthType() == AwsCodeCommitAuthType.HTTPS) {
      AwsCodeCommitHttpsCredentialsDTO credentials =
          (AwsCodeCommitHttpsCredentialsDTO) gitConfigDTO.getAuthentication().getCredentials();
      if (credentials.getType() != AwsCodeCommitHttpsAuthType.ACCESS_KEY_AND_SECRET_KEY) {
        throw new CIStageExecutionException("Unsupported aws code commit connector auth type" + credentials.getType());
      }
    } else {
      throw new CIStageExecutionException(
          "Unsupported aws code commit connector auth" + gitConfigDTO.getAuthentication().getAuthType());
    }
    return envVars;
  }

  private String getGitURL(CodeBase ciCodebase, GitConnectionType connectionType, String url) {
    String gitUrl;
    if (connectionType == GitConnectionType.REPO) {
      gitUrl = url;
    } else if (connectionType == GitConnectionType.ACCOUNT) {
      if (ciCodebase == null) {
        throw new IllegalArgumentException("CI codebase spec is not set");
      }

      if (isEmpty(ciCodebase.getRepoName())) {
        throw new IllegalArgumentException("Repo name is not set in CI codebase spec");
      }

      String repoName = ciCodebase.getRepoName();
      if (url.endsWith(PATH_SEPARATOR)) {
        gitUrl = url + repoName;
      } else {
        gitUrl = url + PATH_SEPARATOR + repoName;
      }
    } else {
      throw new InvalidArgumentsException(
          format("Invalid connection type for git connector: %s", connectionType.toString()), WingsException.USER);
    }

    if (!url.endsWith(GIT_URL_SUFFIX)) {
      gitUrl += GIT_URL_SUFFIX;
    }
    return gitUrl;
  }

  private String getAwsCodeCommitRegion(String url) {
    Pattern r = Pattern.compile(AWS_CODE_COMMIT_URL_REGEX);
    Matcher m = r.matcher(url);

    if (m.find()) {
      return m.group(1);
    } else {
      throw new InvalidRequestException("Url does not have region information");
    }
  }

  private void validateGitConnector(ConnectorDetails gitConnector) {
    if (gitConnector == null) {
      log.error("Git connector is not valid {}", gitConnector);
      throw new InvalidArgumentsException("Git connector is not valid", WingsException.USER);
    }
    if (gitConnector.getConnectorType() != ConnectorType.GIT && gitConnector.getConnectorType() != ConnectorType.GITHUB
        && gitConnector.getConnectorType() != ConnectorType.GITLAB && gitConnector.getConnectorType() != BITBUCKET
        && gitConnector.getConnectorType() != CODECOMMIT) {
      log.error("Git connector ref is not of type git {}", gitConnector.getConnectorType());
      throw new InvalidArgumentsException(
          "Connector type is not from supported connectors list GITHUB, GITLAB, BITBUCKET, CODECOMMIT ",
          WingsException.USER);
    }

    // TODO Validate all

    //    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
    //    if (gitConfigDTO.getGitAuthType() != GitAuthType.HTTP && gitConfigDTO.getGitAuthType() != GitAuthType.SSH) {
    //      log.error("Git connector ref is of invalid auth type {}", gitConnector);
    //      throw new InvalidArgumentsException("Invalid auth provided for git connector", WingsException.USER);
    //    }
  }

  private ConnectorDetails getGitConnector(NGAccess ngAccess, CodeBase codeBase, boolean skipGitClone) {
    if (skipGitClone) {
      return null;
    }

    if (codeBase == null) {
      throw new CIStageExecutionException("CI codebase is mandatory in case git clone is enabled");
    }

    if (codeBase.getConnectorRef() == null) {
      throw new CIStageExecutionException("Git connector is mandatory in case git clone is enabled");
    }
    return connectorUtils.getConnectorDetails(ngAccess, codeBase.getConnectorRef());
  }

  private Map<String, String> getBuildLabels(Ambiance ambiance, K8PodDetails k8PodDetails) {
    final String accountID = AmbianceHelper.getAccountId(ambiance);
    final String orgID = AmbianceHelper.getOrgIdentifier(ambiance);
    final String projectID = AmbianceHelper.getProjectIdentifier(ambiance);
    final String pipelineID = ambiance.getMetadata().getPipelineIdentifier();
    final String pipelineExecutionID = ambiance.getPlanExecutionId();
    final int buildNumber = ambiance.getMetadata().getRunSequence();
    final String stageID = k8PodDetails.getStageID();

    Map<String, String> labels = new HashMap<>();
    if (isLabelAllowed(accountID)) {
      labels.put(ACCOUNT_ID_ATTR, accountID);
    }
    if (isLabelAllowed(orgID)) {
      labels.put(ORG_ID_ATTR, orgID);
    }
    if (isLabelAllowed(projectID)) {
      labels.put(PROJECT_ID_ATTR, projectID);
    }
    if (isLabelAllowed(pipelineID)) {
      labels.put(PIPELINE_ID_ATTR, pipelineID);
    }
    if (isLabelAllowed(pipelineExecutionID)) {
      labels.put(PIPELINE_EXECUTION_ID_ATTR, pipelineExecutionID);
    }
    if (isLabelAllowed(stageID)) {
      labels.put(STAGE_ID_ATTR, stageID);
    }
    if (isLabelAllowed(String.valueOf(buildNumber))) {
      labels.put(BUILD_NUMBER_ATTR, String.valueOf(buildNumber));
    }
    return labels;
  }

  private boolean isLabelAllowed(String label) {
    if (label == null) {
      return false;
    }

    return label.matches(LABEL_REGEX);
  }

  private String getImageName(ConnectorDetails connectorDetails, String imageName) {
    ConnectorType connectorType = connectorDetails.getConnectorType();
    if (connectorType == DOCKER) {
      DockerConnectorDTO dockerConnectorDTO = (DockerConnectorDTO) connectorDetails.getConnectorConfig();
      if (dockerConnectorDTO.getAuth().getAuthType() == DockerAuthType.ANONYMOUS) {
        String dockerRegistryUrl = dockerConnectorDTO.getDockerRegistryUrl();
        try {
          String registryHostName = "";
          URL url = new URL(dockerRegistryUrl);
          registryHostName = url.getHost();
          if (url.getPort() != -1) {
            registryHostName = url.getHost() + ":" + url.getPort();
          }
          return trimTrailingCharacter(registryHostName, '/') + '/' + trimLeadingCharacter(imageName, '/');
        } catch (MalformedURLException e) {
          throw new CIStageExecutionException(
              format("Malformed registryUrl in docker connector id: %s", connectorDetails.getIdentifier()));
        }
      }
    }
    return imageName;
  }
}

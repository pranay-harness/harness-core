package io.harness.stateutils.buildstate;

import static io.harness.common.BuildEnvironmentConstants.DRONE_NETRC_MACHINE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_NETRC_USERNAME;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REMOTE_URL;
import static io.harness.common.CICommonPodConstants.STEP_EXEC;
import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.GIT_URL_SUFFIX;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_WORKSPACE;
import static io.harness.common.CIExecutionConstants.LOCALHOST_IP;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.PATH_SEPARATOR;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.TI_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo.ConnectorConversionInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.CustomSecretVariable;
import io.harness.ci.beans.entities.TIServiceConfig;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.delegate.beans.ci.CIK8BuildTaskParams;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerSecrets;
import io.harness.delegate.beans.ci.pod.HostAliasParams;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector.ImageDetailsWithConnectorBuilder;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitAuthType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.git.GitClientHelper;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.RefObjectUtil;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.product.ci.engine.proto.Execution;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
public class K8BuildSetupUtils {
  @Inject private SecretVariableUtils secretVariableUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ServiceTokenUtils serviceTokenUtils;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private InternalContainerParamsProvider internalContainerParamsProvider;
  @Inject private ExecutionProtobufSerializer protobufSerializer;
  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;
  @Inject private TIServiceConfig tiServiceConfig;
  @Inject CILogServiceUtils logServiceUtils;

  public CIK8BuildTaskParams getCIk8BuildTaskParams(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance, Map<String, String> taskIds) {
    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtil.getSweepingOutputRefObject(ContextElement.podDetails));

    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);

    final String clusterName = k8PodDetails.getClusterName();

    PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());

    ConnectorDetails k8sConnector = connectorUtils.getConnectorDetails(ngAccess, clusterName);
    String workDir = ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getWorkDir();
    CIK8PodParams<CIK8ContainerParams> podParams = getPodParams(ngAccess, k8PodDetails, liteEngineTaskStepInfo,
        liteEngineTaskStepInfo.isUsePVC(), liteEngineTaskStepInfo.getCiCodebase(),
        liteEngineTaskStepInfo.isSkipGitClone(), workDir, taskIds, AmbianceHelper.getAccountId(ambiance));

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
      String workDir, Map<String, String> taskIds, String accountId) {
    PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());
    ConnectorDetails harnessInternalImageRegistryConnectorDetails =
        connectorUtils.getConnectorDetails(ngAccess, ciExecutionServiceConfig.getDefaultInternalImageConnector());
    ConnectorDetails gitConnector = getGitConnector(ngAccess, ciCodebase, skipGitClone);
    Map<String, String> gitEnvVars = getGitEnvVariables(gitConnector, ciCodebase);

    List<CIK8ContainerParams> containerParamsList = getContainerParamsList(k8PodDetails, podSetupInfo, ngAccess,
        harnessInternalImageRegistryConnectorDetails, gitEnvVars, liteEngineTaskStepInfo, taskIds, accountId);

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
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(k8PodDetails.getNamespace())
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
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Map<String, String> taskIds, String accountId) {
    Map<String, String> logEnvVars = getLogServiceEnvVariables(k8PodDetails);
    Map<String, String> tiEnvVars = getTIServiceEnvVariables();
    Map<String, String> commonEnvVars =
        getCommonStepEnvVariables(k8PodDetails, logEnvVars, tiEnvVars, gitEnvVars, podSetupInfo.getWorkDirPath());
    Map<String, ConnectorConversionInfo> stepConnectors =
        ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getStepConnectorRefs();
    Set<String> publishArtifactStepIds =
        ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPublishArtifactStepIds();

    CIK8ContainerParams liteEngineContainerParams =
        createLiteEngineContainerParams(ngAccess, harnessInternalImageRegistryConnectorDetails, stepConnectors,
            publishArtifactStepIds, liteEngineTaskStepInfo, k8PodDetails, podSetupInfo.getStageCpuRequest(),
            podSetupInfo.getStageMemoryRequest(), podSetupInfo.getServiceGrpcPortList(), logEnvVars, tiEnvVars,
            podSetupInfo.getVolumeToMountPath(), podSetupInfo.getWorkDirPath(), taskIds, accountId);

    List<CIK8ContainerParams> containerParams = new ArrayList<>();
    containerParams.add(liteEngineContainerParams);
    // user input containers with custom entry point
    for (ContainerDefinitionInfo containerDefinitionInfo :
        podSetupInfo.getPodSetupParams().getContainerDefinitionInfos()) {
      CIK8ContainerParams cik8ContainerParams = createCIK8ContainerParams(ngAccess, containerDefinitionInfo,
          commonEnvVars, stepConnectors, podSetupInfo.getVolumeToMountPath(), podSetupInfo.getWorkDirPath());
      containerParams.add(cik8ContainerParams);
    }
    return containerParams;
  }

  private CIK8ContainerParams createCIK8ContainerParams(NGAccess ngAccess,
      ContainerDefinitionInfo containerDefinitionInfo, Map<String, String> commonEnvVars,
      Map<String, ConnectorConversionInfo> connectorRefs, Map<String, String> volumeToMountPath, String workDirPath) {
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
    ImageDetailsWithConnectorBuilder imageDetailsWithConnectorBuilder =
        ImageDetailsWithConnector.builder().imageDetails(
            containerDefinitionInfo.getContainerImageDetails().getImageDetails());
    if (containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier() != null) {
      imageDetailsWithConnectorBuilder.imageConnectorDetails(connectorUtils.getConnectorDetails(
          ngAccess, containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier()));
    }

    return CIK8ContainerParams.builder()
        .name(containerDefinitionInfo.getName())
        .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
        .containerType(containerDefinitionInfo.getContainerType())
        .envVars(envVars)
        .containerSecrets(ContainerSecrets.builder()
                              .secretVariableDetails(getSecretVariableDetails(ngAccess, containerDefinitionInfo))
                              .connectorDetailsMap(stepConnectorDetails)
                              .build())
        .commands(containerDefinitionInfo.getCommands())
        .ports(containerDefinitionInfo.getPorts())
        .args(containerDefinitionInfo.getArgs())
        .imageDetailsWithConnector(imageDetailsWithConnectorBuilder.build())
        .volumeToMountPath(volumeToMountPath)
        .workingDir(workDirPath)
        .build();
  }

  private CIK8ContainerParams createLiteEngineContainerParams(NGAccess ngAccess, ConnectorDetails connectorDetails,
      Map<String, ConnectorConversionInfo> connectorRefs, Set<String> publishArtifactStepIds,
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, K8PodDetails k8PodDetails, Integer stageCpuRequest,
      Integer stageMemoryRequest, List<Integer> serviceGrpcPortList, Map<String, String> logEnvVars,
      Map<String, String> tiEnvVars, Map<String, String> volumeToMountPath, String workDirPath,
      Map<String, String> taskIds, String accountId) {
    Map<String, ConnectorDetails> stepConnectorDetails = new HashMap<>();
    if (isNotEmpty(publishArtifactStepIds)) {
      for (String publishArtifactStepId : publishArtifactStepIds) {
        ConnectorDetails publishArtifactConnector =
            connectorUtils.getConnectorDetailsWithConversionInfo(ngAccess, connectorRefs.get(publishArtifactStepId));
        stepConnectorDetails.put(publishArtifactConnector.getIdentifier(), publishArtifactConnector);
      }
    }

    String serializedLiteEngineStepInfo = getSerializedLiteEngineStepInfo(liteEngineTaskStepInfo, taskIds, accountId);
    String serviceToken = serviceTokenUtils.getServiceToken();
    return internalContainerParamsProvider.getLiteEngineContainerParams(connectorDetails, stepConnectorDetails,
        k8PodDetails, serializedLiteEngineStepInfo, serviceToken, stageCpuRequest, stageMemoryRequest,
        serviceGrpcPortList, logEnvVars, tiEnvVars, volumeToMountPath, workDirPath);
  }

  private String getSerializedLiteEngineStepInfo(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Map<String, String> taskIds, String accountId) {
    Execution executionPrototype = protobufSerializer.convertExecutionElement(
        liteEngineTaskStepInfo.getExecutionElementConfig(), liteEngineTaskStepInfo, taskIds);
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
  private List<SecretVariableDetails> getSecretVariableDetails(
      NGAccess ngAccess, ContainerDefinitionInfo containerDefinitionInfo) {
    List<SecretVariableDetails> secretVariableDetails = new ArrayList<>();
    if (isNotEmpty(containerDefinitionInfo.getSecretVariables())) {
      containerDefinitionInfo.getSecretVariables().forEach(secretVariable
          -> secretVariableDetails.add(secretVariableUtils.getSecretVariableDetails(ngAccess, secretVariable)));
    }

    secretVariableDetails.add(secretVariableUtils.getSecretVariableDetails(ngAccess,
        CustomSecretVariable.builder()
            .name(ACCESS_KEY_MINIO_VARIABLE)
            .value(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(ACCESS_KEY_MINIO_VARIABLE).build())
            .build()));
    secretVariableDetails.add(secretVariableUtils.getSecretVariableDetails(ngAccess,
        CustomSecretVariable.builder()
            .name(SECRET_KEY_MINIO_VARIABLE)
            .value(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(SECRET_KEY_MINIO_VARIABLE).build())
            .build()));
    return secretVariableDetails;
  }

  @NotNull
  private Map<String, String> getLogServiceEnvVariables(K8PodDetails k8PodDetails) {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = k8PodDetails.getBuildNumberDetails().getAccountIdentifier();
    final String logServiceBaseUrl = logServiceUtils.getLogServiceConfig().getBaseUrl();

    // Make a call to the log service and get back the token
    String logServiceToken = logServiceUtils.getLogServiceToken(accountID);
    envVars.put(LOG_SERVICE_TOKEN_VARIABLE, logServiceToken);
    envVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, logServiceBaseUrl);

    return envVars;
  }

  @NotNull
  private Map<String, String> getTIServiceEnvVariables() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put(TI_SERVICE_ENDPOINT_VARIABLE, tiServiceConfig.getBaseUrl());

    return envVars;
  }

  @NotNull
  private Map<String, String> getCommonStepEnvVariables(K8PodDetails k8PodDetails, Map<String, String> logEnvVars,
      Map<String, String> tiEnvVars, Map<String, String> gitEnvVars, String workDirPath) {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = k8PodDetails.getBuildNumberDetails().getAccountIdentifier();
    final String projectID = k8PodDetails.getBuildNumberDetails().getProjectIdentifier();
    final String orgID = k8PodDetails.getBuildNumberDetails().getOrgIdentifier();
    final Long buildNumber = k8PodDetails.getBuildNumberDetails().getBuildNumber();
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
    envVars.put(HARNESS_BUILD_ID_VARIABLE, buildNumber.toString());
    envVars.put(HARNESS_STAGE_ID_VARIABLE, stageID);
    return envVars;
  }

  private Map<String, String> getGitEnvVariables(ConnectorDetails gitConnector, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    if (gitConnector == null) {
      return envVars;
    }

    validateGitConnector(gitConnector);

    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
    String gitUrl = getGitUrl(gitConfigDTO, ciCodebase);
    String domain = GitClientHelper.getGitSCM(gitUrl);

    envVars.put(DRONE_REMOTE_URL, gitUrl);
    envVars.put(DRONE_NETRC_MACHINE, domain);
    if (gitConfigDTO.getGitAuthType() == GitAuthType.HTTP) {
      GitHTTPAuthenticationDTO gitAuth = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
      envVars.put(DRONE_NETRC_USERNAME, gitAuth.getUsername());
    }
    return envVars;
  }

  private String getGitUrl(GitConfigDTO gitConfigDTO, CodeBase ciCodebase) {
    String gitUrl;
    String url = gitConfigDTO.getUrl();
    if (gitConfigDTO.getGitConnectionType() == GitConnectionType.REPO) {
      gitUrl = url;
    } else if (gitConfigDTO.getGitConnectionType() == GitConnectionType.ACCOUNT) {
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
          format("Invalid connection type for git connector: %s", gitConfigDTO.getGitConnectionType().toString()),
          WingsException.USER);
    }

    if (!url.endsWith(GIT_URL_SUFFIX)) {
      gitUrl += GIT_URL_SUFFIX;
    }
    return gitUrl;
  }

  private void validateGitConnector(ConnectorDetails gitConnector) {
    if (gitConnector == null) {
      log.error("Git connector is not valid {}", gitConnector);
      throw new InvalidArgumentsException("Git connector is not valid", WingsException.USER);
    }
    if (gitConnector.getConnectorType() != ConnectorType.GIT) {
      log.error("Git connector ref is not of type git {}", gitConnector);
      throw new InvalidArgumentsException("Connector type for git connector is not GIT", WingsException.USER);
    }

    GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
    if (gitConfigDTO.getGitAuthType() != GitAuthType.HTTP && gitConfigDTO.getGitAuthType() != GitAuthType.SSH) {
      log.error("Git connector ref is of invalid auth type {}", gitConnector);
      throw new InvalidArgumentsException("Invalid auth provided for git connector", WingsException.USER);
    }
  }

  private ConnectorDetails getGitConnector(NGAccess ngAccess, CodeBase codeBase, boolean skipGitClone) {
    if (skipGitClone) {
      return null;
    }

    if (codeBase == null) {
      throw new IllegalArgumentException("CI codebase is not set");
    }

    if (codeBase.getConnectorRef() == null) {
      throw new IllegalArgumentException("Git connector is not set in CI codebase");
    }
    return connectorUtils.getConnectorDetails(ngAccess, codeBase.getConnectorRef());
  }
}

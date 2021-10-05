package io.harness.stateutils.buildstate;

import static io.harness.beans.serializer.RunTimeInputHandler.UNRESOLVED_PARAMETER;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveIntegerParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODEBASE;
import static io.harness.beans.sweepingoutputs.CISweepingOutputNames.CODE_BASE_CONNECTOR_REF;
import static io.harness.beans.sweepingoutputs.ContainerPortDetails.PORT_DETAILS;
import static io.harness.beans.sweepingoutputs.PodCleanupDetails.CLEANUP_DETAILS;
import static io.harness.common.BuildEnvironmentConstants.DRONE_AWS_REGION;
import static io.harness.common.BuildEnvironmentConstants.DRONE_BUILD_EVENT;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AUTHOR;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AUTHOR_AVATAR;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_AUTHOR_EMAIL;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_BEFORE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_BRANCH;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_REF;
import static io.harness.common.BuildEnvironmentConstants.DRONE_COMMIT_SHA;
import static io.harness.common.BuildEnvironmentConstants.DRONE_NETRC_MACHINE;
import static io.harness.common.BuildEnvironmentConstants.DRONE_NETRC_PORT;
import static io.harness.common.BuildEnvironmentConstants.DRONE_NETRC_USERNAME;
import static io.harness.common.BuildEnvironmentConstants.DRONE_REMOTE_URL;
import static io.harness.common.BuildEnvironmentConstants.DRONE_SOURCE_BRANCH;
import static io.harness.common.BuildEnvironmentConstants.DRONE_TAG;
import static io.harness.common.BuildEnvironmentConstants.DRONE_TARGET_BRANCH;
import static io.harness.common.CIExecutionConstants.ACCOUNT_ID_ATTR;
import static io.harness.common.CIExecutionConstants.AWS_CODE_COMMIT_URL_REGEX;
import static io.harness.common.CIExecutionConstants.BUILD_NUMBER_ATTR;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_LOG_PREFIX_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PIPELINE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_SERVICE_LOG_KEY_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_WORKSPACE;
import static io.harness.common.CIExecutionConstants.LABEL_REGEX;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.ORG_ID_ATTR;
import static io.harness.common.CIExecutionConstants.PIPELINE_EXECUTION_ID_ATTR;
import static io.harness.common.CIExecutionConstants.PIPELINE_ID_ATTR;
import static io.harness.common.CIExecutionConstants.POD_MAX_WAIT_UNTIL_READY_SECS;
import static io.harness.common.CIExecutionConstants.PROJECT_ID_ATTR;
import static io.harness.common.CIExecutionConstants.STAGE_ID_ATTR;
import static io.harness.common.CIExecutionConstants.TI_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.TI_SERVICE_TOKEN_VARIABLE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.BITBUCKET;
import static io.harness.delegate.beans.connector.ConnectorType.CODECOMMIT;
import static io.harness.delegate.beans.connector.ConnectorType.GIT;
import static io.harness.delegate.beans.connector.ConnectorType.GITHUB;
import static io.harness.delegate.beans.connector.ConnectorType.GITLAB;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo.ConnectorConversionInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.CodeBaseConnectorRefSweepingOutput;
import io.harness.beans.sweepingoutputs.CodebaseSweepingOutput;
import io.harness.beans.sweepingoutputs.ContainerPortDetails;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.PodCleanupDetails;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.integrationstage.IntegrationStageUtils;
import io.harness.delegate.beans.ci.CIK8BuildTaskParams;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.CIK8ContainerParams;
import io.harness.delegate.beans.ci.pod.CIK8PodParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ContainerSecrets;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsAuthType;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitHttpsCredentialsDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitHTTPAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.github.GithubHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabHttpCredentialsDTO;
import io.harness.exception.ConnectorNotFoundException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.git.GitClientHelper;
import io.harness.k8s.model.ImageDetails;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.rbac.PipelineRbacHelper;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import io.harness.tiserviceclient.TIServiceUtils;
import io.harness.util.GithubApiFunctor;
import io.harness.util.GithubApiTokenEvaluator;
import io.harness.util.LiteEngineSecretEvaluator;
import io.harness.utils.IdentifierRefHelper;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.extended.ci.codebase.CodeBase;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.jetbrains.annotations.NotNull;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class K8BuildSetupUtils {
  @Inject private SecretUtils secretUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private InternalContainerParamsProvider internalContainerParamsProvider;
  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;
  @Inject CILogServiceUtils logServiceUtils;
  @Inject TIServiceUtils tiServiceUtils;
  @Inject private PipelineRbacHelper pipelineRbacHelper;
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;

  public CIK8BuildTaskParams getCIk8BuildTaskParams(LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance,
      Map<String, String> taskIds, String logPrefix, Map<String, String> stepLogKeys) {
    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(ContextElement.podDetails));

    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    Infrastructure infrastructure = liteEngineTaskStepInfo.getInfrastructure();

    if (infrastructure == null || ((K8sDirectInfraYaml) infrastructure).getSpec() == null) {
      throw new CIStageExecutionException("Input infrastructure can not be empty");
    }

    K8sDirectInfraYaml k8sDirectInfraYaml = (K8sDirectInfraYaml) infrastructure;

    final String clusterName = k8sDirectInfraYaml.getSpec().getConnectorRef();
    Map<String, String> annotations = resolveMapParameter(
        "annotations", "K8BuildInfra", "stageSetup", k8sDirectInfraYaml.getSpec().getAnnotations(), false);
    Map<String, String> labels = resolveMapParameter(
        "annotations", "K8BuildInfra", "stageSetup", k8sDirectInfraYaml.getSpec().getLabels(), false);

    Integer stageRunAsUser = resolveIntegerParameter(k8sDirectInfraYaml.getSpec().getRunAsUser(), null);
    String resolveStringParameter = resolveStringParameter(
        "serviceAccountName", null, "infrastructure", k8sDirectInfraYaml.getSpec().getServiceAccountName(), false);
    String serviceAccountName = null;
    if (resolveStringParameter != null && !resolveStringParameter.equals(UNRESOLVED_PARAMETER)) {
      serviceAccountName = resolveStringParameter;
    }
    PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());

    ConnectorDetails k8sConnector = connectorUtils.getConnectorDetails(ngAccess, clusterName);
    String workDir = ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getWorkDir();
    CIK8PodParams<CIK8ContainerParams> podParams =
        getPodParams(ngAccess, k8PodDetails, liteEngineTaskStepInfo, liteEngineTaskStepInfo.isUsePVC(),
            liteEngineTaskStepInfo.getCiCodebase(), liteEngineTaskStepInfo.isSkipGitClone(), logPrefix, ambiance,
            annotations, labels, stageRunAsUser, serviceAccountName);

    log.info("Created pod params for pod name [{}]", podSetupInfo.getName());
    return CIK8BuildTaskParams.builder()
        .k8sConnector(k8sConnector)
        .cik8PodParams(podParams)
        .podMaxWaitUntilReadySecs(getPodWaitUntilReadTimeout(k8sDirectInfraYaml))
        .build();
  }

  private int getPodWaitUntilReadTimeout(K8sDirectInfraYaml k8sDirectInfraYaml) {
    ParameterField<String> timeout = k8sDirectInfraYaml.getSpec().getInitTimeout();

    int podWaitUntilReadyTimeout = POD_MAX_WAIT_UNTIL_READY_SECS;
    if (timeout != null && timeout.fetchFinalValue() != null && isNotEmpty((String) timeout.fetchFinalValue())) {
      long timeoutInMillis = Timeout.fromString((String) timeout.fetchFinalValue()).getTimeoutInMillis();
      podWaitUntilReadyTimeout = (int) (timeoutInMillis / 1000);
    }
    return podWaitUntilReadyTimeout;
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
      String logPrefix, Ambiance ambiance, Map<String, String> annotations, Map<String, String> labels,
      Integer stageRunAsUser, String serviceAccountName) {
    PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());
    ConnectorDetails harnessInternalImageConnector = null;
    if (isNotEmpty(ciExecutionServiceConfig.getDefaultInternalImageConnector())) {
      harnessInternalImageConnector = getDefaultInternalConnector(ngAccess);
    }

    ConnectorDetails gitConnector = getGitConnector(ngAccess, ciCodebase, skipGitClone);
    Map<String, String> gitEnvVars = getGitEnvVariables(gitConnector, ciCodebase);
    Map<String, String> runtimeCodebaseVars = getRuntimeCodebaseVars(ambiance);

    List<CIK8ContainerParams> containerParamsList = getContainerParamsList(k8PodDetails, podSetupInfo, ngAccess,
        harnessInternalImageConnector, gitEnvVars, runtimeCodebaseVars, liteEngineTaskStepInfo, logPrefix, ambiance);

    CIK8ContainerParams setupAddOnContainerParams = internalContainerParamsProvider.getSetupAddonContainerParams(
        harnessInternalImageConnector, podSetupInfo.getVolumeToMountPath(), podSetupInfo.getWorkDirPath());

    // Service identifier usage in host alias requires that service identifier does not have capital letter characters
    // or _. For now, removing host alias usage otherwise pod creation itself fails.
    //
    //    List<HostAliasParams> hostAliasParamsList = new ArrayList<>();
    //    if (podSetupInfo.getServiceIdList() != null) {
    //      hostAliasParamsList.add(
    //          HostAliasParams.builder().ipAddress(LOCALHOST_IP).hostnameList(podSetupInfo.getServiceIdList()).build());
    //    }

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

    Map<String, String> buildLabels = getBuildLabels(ambiance, k8PodDetails);
    if (isNotEmpty(labels)) {
      buildLabels.putAll(labels);
    }

    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(k8sDirectInfraYaml.getSpec().getNamespace())
        .labels(buildLabels)
        .serviceAccountName(serviceAccountName)
        .annotations(annotations)
        .gitConnector(gitConnector)
        .containerParamsList(containerParamsList)
        .pvcParamList(pvcParamsList)
        .initContainerParamsList(singletonList(setupAddOnContainerParams))
        .runAsUser(stageRunAsUser)
        .build();
  }

  private Map<String, String> getRuntimeCodebaseVars(Ambiance ambiance) {
    Map<String, String> codebaseRuntimeVars = new HashMap<>();

    OptionalSweepingOutput optionalSweepingOutput =
        executionSweepingOutputResolver.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(CODEBASE));
    if (!optionalSweepingOutput.isFound()) {
      return codebaseRuntimeVars;
    }

    CodebaseSweepingOutput codebaseSweeping = (CodebaseSweepingOutput) optionalSweepingOutput.getOutput();
    if (isNotEmpty(codebaseSweeping.getBranch())) {
      codebaseRuntimeVars.put(DRONE_COMMIT_BRANCH, codebaseSweeping.getSourceBranch());
    }

    if (!isEmpty(codebaseSweeping.getTag())) {
      codebaseRuntimeVars.put(DRONE_TAG, codebaseSweeping.getTag());
      codebaseRuntimeVars.put(DRONE_BUILD_EVENT, "tag");
    }

    if (!isEmpty(codebaseSweeping.getTargetBranch())) {
      codebaseRuntimeVars.put(DRONE_TARGET_BRANCH, codebaseSweeping.getTargetBranch());
    }

    if (!isEmpty(codebaseSweeping.getSourceBranch())) {
      codebaseRuntimeVars.put(DRONE_SOURCE_BRANCH, codebaseSweeping.getSourceBranch());
    }

    if (!isEmpty(codebaseSweeping.getSourceBranch())) {
      codebaseRuntimeVars.put(DRONE_SOURCE_BRANCH, codebaseSweeping.getSourceBranch());
    }
    if (!isEmpty(codebaseSweeping.getGitUserEmail())) {
      codebaseRuntimeVars.put(DRONE_COMMIT_AUTHOR_EMAIL, codebaseSweeping.getGitUserEmail());
    }
    if (!isEmpty(codebaseSweeping.getGitUserAvatar())) {
      codebaseRuntimeVars.put(DRONE_COMMIT_AUTHOR_AVATAR, codebaseSweeping.getGitUserAvatar());
    }

    if (!isEmpty(codebaseSweeping.getGitUserId())) {
      codebaseRuntimeVars.put(DRONE_COMMIT_AUTHOR, codebaseSweeping.getGitUserId());
    }
    if (isNotEmpty(codebaseSweeping.getBaseCommitSha())) {
      codebaseRuntimeVars.put(DRONE_COMMIT_BEFORE, codebaseSweeping.getBaseCommitSha());
    }
    if (isNotEmpty(codebaseSweeping.getCommitRef())) {
      codebaseRuntimeVars.put(DRONE_COMMIT_REF, codebaseSweeping.getCommitRef());
    }
    if (isNotEmpty(codebaseSweeping.getCommitSha())) {
      codebaseRuntimeVars.put(DRONE_COMMIT_SHA, codebaseSweeping.getCommitSha());
    }

    return codebaseRuntimeVars;
  }

  public List<CIK8ContainerParams> getContainerParamsList(K8PodDetails k8PodDetails, PodSetupInfo podSetupInfo,
      NGAccess ngAccess, ConnectorDetails harnessInternalImageConnector, Map<String, String> gitEnvVars,
      Map<String, String> runtimeCodebaseVars, LiteEngineTaskStepInfo liteEngineTaskStepInfo, String logPrefix,
      Ambiance ambiance) {
    String accountId = AmbianceUtils.getAccountId(ambiance);
    Map<String, String> logEnvVars = getLogServiceEnvVariables(k8PodDetails, accountId);
    Map<String, String> tiEnvVars = getTIServiceEnvVariables(accountId);
    Map<String, String> commonEnvVars = getCommonStepEnvVariables(
        k8PodDetails, gitEnvVars, runtimeCodebaseVars, podSetupInfo.getWorkDirPath(), logPrefix, ambiance);
    Map<String, ConnectorConversionInfo> stepConnectors =
        ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getStepConnectorRefs();

    LiteEngineSecretEvaluator liteEngineSecretEvaluator =
        LiteEngineSecretEvaluator.builder().secretUtils(secretUtils).build();
    List<SecretVariableDetails> secretVariableDetails =
        liteEngineSecretEvaluator.resolve(liteEngineTaskStepInfo, ngAccess, ambiance.getExpressionFunctorToken());
    checkSecretAccess(ambiance, secretVariableDetails, accountId, AmbianceUtils.getProjectIdentifier(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance));

    Map<String, ConnectorDetails> githubApiTokenFunctorConnectors =
        resolveGitAppFunctor(ngAccess, liteEngineTaskStepInfo, ambiance);

    CIK8ContainerParams liteEngineContainerParams = createLiteEngineContainerParams(harnessInternalImageConnector,
        k8PodDetails, podSetupInfo.getStageCpuRequest(), podSetupInfo.getStageMemoryRequest(), logEnvVars, tiEnvVars,
        podSetupInfo.getVolumeToMountPath(), podSetupInfo.getWorkDirPath(), logPrefix, ambiance);

    List<CIK8ContainerParams> containerParams = new ArrayList<>();
    containerParams.add(liteEngineContainerParams);
    // user input containers with custom entry point
    consumePortDetails(ambiance, podSetupInfo.getPodSetupParams().getContainerDefinitionInfos());
    for (ContainerDefinitionInfo containerDefinitionInfo :
        podSetupInfo.getPodSetupParams().getContainerDefinitionInfos()) {
      CIK8ContainerParams cik8ContainerParams = createCIK8ContainerParams(ngAccess, containerDefinitionInfo,
          harnessInternalImageConnector, commonEnvVars, stepConnectors, podSetupInfo.getVolumeToMountPath(),
          podSetupInfo.getWorkDirPath(), logPrefix, secretVariableDetails, githubApiTokenFunctorConnectors);
      containerParams.add(cik8ContainerParams);
    }
    return containerParams;
  }

  private Map<String, ConnectorDetails> resolveGitAppFunctor(
      NGAccess ngAccess, LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    String codeBaseConnectorRef = null;
    if (liteEngineTaskStepInfo.getCiCodebase() != null) {
      codeBaseConnectorRef = liteEngineTaskStepInfo.getCiCodebase().getConnectorRef();
      if (isNotEmpty(codeBaseConnectorRef)) {
        executionSweepingOutputResolver.consume(ambiance, CODE_BASE_CONNECTOR_REF,
            CodeBaseConnectorRefSweepingOutput.builder().codeBaseConnectorRef(codeBaseConnectorRef).build(),
            StepOutcomeGroup.STAGE.name());
      }
    }
    GithubApiTokenEvaluator githubApiTokenEvaluator =
        GithubApiTokenEvaluator.builder()
            .githubApiFunctorConfig(GithubApiFunctor.Config.builder()
                                        .codeBaseConnectorRef(codeBaseConnectorRef)
                                        .fetchConnector(true)
                                        .build())
            .connectorUtils(connectorUtils)
            .build();

    return githubApiTokenEvaluator.resolve(liteEngineTaskStepInfo, ngAccess, ambiance.getExpressionFunctorToken());
  }

  private void checkSecretAccess(Ambiance ambiance, List<SecretVariableDetails> secretVariableDetails,
      String accountIdentifier, String projectIdentifier, String orgIdentifier) {
    List<EntityDetail> entityDetails =
        secretVariableDetails.stream()
            .map(secretVariableDetail -> {
              return createEntityDetails(secretVariableDetail.getSecretVariableDTO().getSecret().getIdentifier(),
                  accountIdentifier, projectIdentifier, orgIdentifier);
            })
            .collect(Collectors.toList());

    if (isNotEmpty(entityDetails)) {
      pipelineRbacHelper.checkRuntimePermissions(ambiance, entityDetails, false);
    }
  }

  private EntityDetail createEntityDetails(
      String secretIdentifier, String accountIdentifier, String projectIdentifier, String orgIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(secretIdentifier, accountIdentifier, orgIdentifier, projectIdentifier);
    return EntityDetail.builder().entityRef(connectorRef).type(EntityType.SECRETS).build();
  }

  private void consumePortDetails(Ambiance ambiance, List<ContainerDefinitionInfo> containerDefinitionInfos) {
    Map<String, List<Integer>> portDetails = containerDefinitionInfos.stream().collect(
        Collectors.toMap(ContainerDefinitionInfo::getStepIdentifier, ContainerDefinitionInfo::getPorts));

    executionSweepingOutputResolver.consume(ambiance, PORT_DETAILS,
        ContainerPortDetails.builder().portDetails(portDetails).build(), StepOutcomeGroup.STAGE.name());
  }

  private CIK8ContainerParams createCIK8ContainerParams(NGAccess ngAccess,
      ContainerDefinitionInfo containerDefinitionInfo, ConnectorDetails harnessInternalImageConnector,
      Map<String, String> commonEnvVars, Map<String, ConnectorConversionInfo> connectorRefs,
      Map<String, String> volumeToMountPath, String workDirPath, String logPrefix,
      List<SecretVariableDetails> secretVariableDetails,
      Map<String, ConnectorDetails> githubApiTokenFunctorConnectors) {
    Map<String, String> envVars = new HashMap<>();
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
    }
    String fullyQualifiedImageName = getFullyQualifiedImageName(connectorDetails, harnessInternalImageConnector,
        containerDefinitionInfo.isHarnessManagedImage(), imageDetails.getName());
    imageDetails.setName(fullyQualifiedImageName);
    ImageDetailsWithConnector imageDetailsWithConnector =
        ImageDetailsWithConnector.builder().imageConnectorDetails(connectorDetails).imageDetails(imageDetails).build();

    List<SecretVariableDetails> containerSecretVariableDetails =
        getSecretVariableDetails(ngAccess, containerDefinitionInfo, secretVariableDetails);

    Map<String, String> envVarsWithSecretRef = removeEnvVarsWithSecretRef(envVars);
    envVars.putAll(commonEnvVars); //  commonEnvVars needs to be put in end because they overrides webhook parameters
    if (containerDefinitionInfo.getContainerType() == CIContainerType.SERVICE) {
      envVars.put(HARNESS_SERVICE_LOG_KEY_VARIABLE,
          format("%s/serviceId:%s", logPrefix, containerDefinitionInfo.getStepIdentifier()));
    }

    CIK8ContainerParams cik8ContainerParams =
        CIK8ContainerParams.builder()
            .name(containerDefinitionInfo.getName())
            .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
            .containerType(containerDefinitionInfo.getContainerType())
            .envVars(envVars)
            .envVarsWithSecretRef(envVarsWithSecretRef)
            .containerSecrets(ContainerSecrets.builder()
                                  .secretVariableDetails(containerSecretVariableDetails)
                                  .connectorDetailsMap(stepConnectorDetails)
                                  .functorConnectors(githubApiTokenFunctorConnectors)
                                  .build())
            .commands(containerDefinitionInfo.getCommands())
            .ports(containerDefinitionInfo.getPorts())
            .args(containerDefinitionInfo.getArgs())
            .imageDetailsWithConnector(imageDetailsWithConnector)
            .volumeToMountPath(volumeToMountPath)
            .privileged(containerDefinitionInfo.isPrivileged())
            .runAsUser(containerDefinitionInfo.getRunAsUser())
            .imagePullPolicy(containerDefinitionInfo.getImagePullPolicy())
            .build();
    if (containerDefinitionInfo.getContainerType() != CIContainerType.SERVICE) {
      cik8ContainerParams.setWorkingDir(workDirPath);
    }
    return cik8ContainerParams;
  }

  private Map<String, String> removeEnvVarsWithSecretRef(Map<String, String> envVars) {
    HashMap<String, String> hashMap = new HashMap<>();
    final Map<String, String> secretEnvVariables =
        envVars.entrySet()
            .stream()
            .filter(entry -> entry.getValue().contains("ngSecretManager"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    envVars.entrySet().removeAll(secretEnvVariables.entrySet());

    return secretEnvVariables;
  }

  private String getFullyQualifiedImageName(ConnectorDetails connectorDetails,
      ConnectorDetails harnessInternalImageConnector, boolean isHarnessManagedImage, String imageName) {
    ConnectorDetails imgConnector = connectorDetails;
    if (isHarnessManagedImage) {
      imgConnector = harnessInternalImageConnector;
    }

    return IntegrationStageUtils.getFullyQualifiedImageName(imageName, imgConnector);
  }

  private CIK8ContainerParams createLiteEngineContainerParams(ConnectorDetails connectorDetails,
      K8PodDetails k8PodDetails, Integer stageCpuRequest, Integer stageMemoryRequest, Map<String, String> logEnvVars,
      Map<String, String> tiEnvVars, Map<String, String> volumeToMountPath, String workDirPath, String logPrefix,
      Ambiance ambiance) {
    Map<String, ConnectorDetails> stepConnectorDetails = new HashMap<>();

    return internalContainerParamsProvider.getLiteEngineContainerParams(connectorDetails, stepConnectorDetails,
        k8PodDetails, stageCpuRequest, stageMemoryRequest, logEnvVars, tiEnvVars, volumeToMountPath, workDirPath,
        logPrefix, ambiance);
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

    RetryPolicy<Object> retryPolicy =
        getRetryPolicy(format("[Retrying failed call to fetch log service token attempt: {}"),
            format("Failed to fetch log service token after retrying {} times"));

    // Make a call to the log service and get back the token
    String logServiceToken =
        Failsafe.with(retryPolicy).get(() -> { return logServiceUtils.getLogServiceToken(accountID); });

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
  private Map<String, String> getCommonStepEnvVariables(K8PodDetails k8PodDetails, Map<String, String> gitEnvVars,
      Map<String, String> runtimeCodebaseVars, String workDirPath, String logPrefix, Ambiance ambiance) {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = AmbianceUtils.getAccountId(ambiance);
    final String orgID = AmbianceUtils.getOrgIdentifier(ambiance);
    final String projectID = AmbianceUtils.getProjectIdentifier(ambiance);
    final String pipelineID = ambiance.getMetadata().getPipelineIdentifier();
    final int buildNumber = ambiance.getMetadata().getRunSequence();
    final String stageID = k8PodDetails.getStageID();

    // Add git connector environment variables
    envVars.putAll(gitEnvVars);

    // Add runtime git vars, i.e. manual pull request execution data.
    envVars.putAll(runtimeCodebaseVars);

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
      validateGithubConnectorAuth(gitConfigDTO);
      envVars = retrieveGitSCMEnvVar(ciCodebase, gitConfigDTO.getConnectionType(), gitConfigDTO.getUrl());
    } else if (gitConnector.getConnectorType() == GITLAB) {
      GitlabConnectorDTO gitConfigDTO = (GitlabConnectorDTO) gitConnector.getConnectorConfig();
      validateGitlabConnectorAuth(gitConfigDTO);
      envVars = retrieveGitSCMEnvVar(ciCodebase, gitConfigDTO.getConnectionType(), gitConfigDTO.getUrl());
    } else if (gitConnector.getConnectorType() == BITBUCKET) {
      BitbucketConnectorDTO gitConfigDTO = (BitbucketConnectorDTO) gitConnector.getConnectorConfig();
      validateBitbucketConnectorAuth(gitConfigDTO);
      envVars = retrieveGitSCMEnvVar(ciCodebase, gitConfigDTO.getConnectionType(), gitConfigDTO.getUrl());
    } else if (gitConnector.getConnectorType() == CODECOMMIT) {
      AwsCodeCommitConnectorDTO gitConfigDTO = (AwsCodeCommitConnectorDTO) gitConnector.getConnectorConfig();
      envVars = retrieveAwsCodeCommitEnvVar(gitConfigDTO, ciCodebase);
    } else if (gitConnector.getConnectorType() == GIT) {
      GitConfigDTO gitConfigDTO = (GitConfigDTO) gitConnector.getConnectorConfig();
      envVars = retrieveGitEnvVar(gitConfigDTO, ciCodebase);
    } else {
      throw new CIStageExecutionException("Unsupported git connector type" + gitConnector.getConnectorType());
    }

    return envVars;
  }

  private Map<String, String> retrieveGitSCMEnvVar(CodeBase ciCodebase, GitConnectionType connectionType, String url) {
    Map<String, String> envVars = new HashMap<>();
    String gitUrl = IntegrationStageUtils.getGitURL(ciCodebase, connectionType, url);
    String domain = GitClientHelper.getGitSCM(gitUrl);
    String port = GitClientHelper.getGitSCMPort(gitUrl);
    if (port != null) {
      envVars.put(DRONE_NETRC_PORT, port);
    }

    envVars.put(DRONE_REMOTE_URL, gitUrl);
    envVars.put(DRONE_NETRC_MACHINE, domain);
    return envVars;
  }

  private void validateGithubConnectorAuth(GithubConnectorDTO gitConfigDTO) {
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
  }

  private void validateBitbucketConnectorAuth(BitbucketConnectorDTO gitConfigDTO) {
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
  }

  private void validateGitlabConnectorAuth(GitlabConnectorDTO gitConfigDTO) {
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
  }

  private Map<String, String> retrieveGitEnvVar(GitConfigDTO gitConfigDTO, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    String gitUrl =
        IntegrationStageUtils.getGitURL(ciCodebase, gitConfigDTO.getGitConnectionType(), gitConfigDTO.getUrl());
    String domain = GitClientHelper.getGitSCM(gitUrl);

    envVars.put(DRONE_REMOTE_URL, gitUrl);
    envVars.put(DRONE_NETRC_MACHINE, domain);
    switch (gitConfigDTO.getGitAuthType()) {
      case HTTP:
        GitHTTPAuthenticationDTO gitAuth = (GitHTTPAuthenticationDTO) gitConfigDTO.getGitAuth();
        envVars.put(DRONE_NETRC_USERNAME, gitAuth.getUsername());
        break;
      case SSH:
        break;
      default:
        throw new CIStageExecutionException("Unsupported bitbucket connector auth" + gitConfigDTO.getGitAuthType());
    }
    return envVars;
  }

  private Map<String, String> retrieveAwsCodeCommitEnvVar(AwsCodeCommitConnectorDTO gitConfigDTO, CodeBase ciCodebase) {
    Map<String, String> envVars = new HashMap<>();
    GitConnectionType gitConnectionType =
        gitConfigDTO.getUrlType() == AwsCodeCommitUrlType.REPO ? GitConnectionType.REPO : GitConnectionType.ACCOUNT;
    String gitUrl = IntegrationStageUtils.getGitURL(ciCodebase, gitConnectionType, gitConfigDTO.getUrl());

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
    if (gitConnector.getConnectorType() != GIT && gitConnector.getConnectorType() != ConnectorType.GITHUB
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
    final String accountID = AmbianceUtils.getAccountId(ambiance);
    final String orgID = AmbianceUtils.getOrgIdentifier(ambiance);
    final String projectID = AmbianceUtils.getProjectIdentifier(ambiance);
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

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }

  private ConnectorDetails getDefaultInternalConnector(NGAccess ngAccess) {
    ConnectorDetails connectorDetails = null;
    if (isNotEmpty(ciExecutionServiceConfig.getDefaultInternalImageConnector())) {
      try {
        connectorDetails =
            connectorUtils.getConnectorDetails(ngAccess, ciExecutionServiceConfig.getDefaultInternalImageConnector());
      } catch (ConnectorNotFoundException e) {
        log.info("Default harness image connector does not exist: {}", e.getMessage());
        connectorDetails = null;
      }
    }
    return connectorDetails;
  }
}

package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.govern.Switch.noop;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.AccountType.COMMUNITY;
import static software.wings.beans.AccountType.ESSENTIALS;
import static software.wings.beans.FeatureName.CV_SUCCEED_FOR_ANOMALY;
import static software.wings.common.TemplateExpressionProcessor.checkFieldTemplatized;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;
import static software.wings.common.VerificationConstants.GA_PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.PER_MINUTE_CV_STATES;
import static software.wings.common.VerificationConstants.URL_STRING;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.ThirdPartyApiCallLog.PAYLOAD;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.PREDICTIVE;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.model.K8sPod;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.time.Timestamp;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.slf4j.Logger;
import software.wings.api.AmiServiceSetupElement;
import software.wings.api.DeploymentType;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PcfInstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.PhaseExecutionData;
import software.wings.api.ServiceElement;
import software.wings.api.k8s.K8sElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.TemplateExpression;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.ThirdPartyApiCallLog.FieldType;
import software.wings.service.impl.ThirdPartyApiCallLog.ThirdPartyApiCallField;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData.ContinuousVerificationExecutionMetaDataBuilder;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.StateExecutionService.CurrentPhase;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.aws.manager.AwsAsgHelperServiceManager;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CVActivityLogService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.k8s.K8sStateHelper;
import software.wings.stencils.DefaultValue;
import software.wings.verification.VerificationDataAnalysisResponse;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Created by rsingh on 7/6/17.
 */
@FieldNameConstants(innerTypeName = "AbstractAnalysisStateKeys")
public abstract class AbstractAnalysisState extends State {
  private static final SecureRandom random = new SecureRandom();
  // only use it in the new instance API.
  private static final String DEFAULT_HOSTNAME_TEMPLATE = "${instanceDetails.hostName}";
  public static final String RETRYING_VERIFICATION_STATE_MSG = "Retrying verification state..";
  public static final String START_TIME_PLACE_HOLDER = "$startTime";
  public static final String END_TIME_PLACE_HOLDER = "$endTime";
  public static final String HOST_NAME_PLACE_HOLDER = "$hostName";
  public static final int MAX_SAMPLING_SIZE_PER_GROUP = 10;

  protected String timeDuration;
  protected String comparisonStrategy;
  protected String tolerance;
  protected String predictiveHistoryMinutes;
  protected String initialAnalysisDelay = "2m";

  private static final int DEFAULT_VERIFICATION_STATE_TIMEOUT_MILLIS = 3 * 60 * 60 * 1000; // 3 hours
  private static final int TIMEOUT_BUFFER = 150; // 150 Minutes.
  private static final int MAX_WORKFLOW_TIMEOUT = 4 * 60; // 4 hours

  @Inject protected WorkflowExecutionService workflowExecutionService;
  @Inject protected WaitNotifyEngine waitNotifyEngine;
  @Inject protected SettingsService settingsService;
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected AppService appService;
  @Inject protected DelegateService delegateService;
  @Inject protected SecretManager secretManager;
  @Inject @SchemaIgnore protected MainConfiguration configuration;
  @Inject @SchemaIgnore protected ContainerInstanceHandler containerInstanceHandler;
  @Inject @SchemaIgnore protected InfrastructureMappingService infraMappingService;
  @Inject protected TemplateExpressionProcessor templateExpressionProcessor;
  @Inject @SchemaIgnore protected WorkflowExecutionBaselineService workflowExecutionBaselineService;
  @Inject @SchemaIgnore protected ContinuousVerificationService continuousVerificationService;
  @Inject @SchemaIgnore private AwsHelperService awsHelperService;
  @Inject @SchemaIgnore private DelegateProxyFactory delegateProxyFactory;
  @Inject protected FeatureFlagService featureFlagService;
  @Inject private HostService hostService;
  @Inject protected StateExecutionService stateExecutionService;
  @Inject @SchemaIgnore protected ServiceResourceService serviceResourceService;
  @Inject @SchemaIgnore protected K8sStateHelper k8sStateHelper;
  @Inject private transient ExpressionEvaluator evaluator;
  @Inject private AccountService accountService;
  @Inject private AwsAsgHelperServiceManager awsAsgHelperServiceManager;
  @Inject protected CVActivityLogService cvActivityLogService;

  protected String hostnameField;

  protected String hostnameTemplate;

  protected boolean includePreviousPhaseNodes;

  @Attributes(title = "Analysis Time duration (in minutes)")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  protected String getTimeDuration(ExecutionContext context) {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return getResolvedFieldValue(context, AbstractAnalysisStateKeys.timeDuration, timeDuration);
  }

  protected boolean isEligibleForPerMinuteTask(String accountId) {
    return getComparisonStrategy() == PREDICTIVE
        || (featureFlagService.isEnabled(FeatureName.CV_DATA_COLLECTION_JOB, accountId)
               && (PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()))))
        || GA_PER_MINUTE_CV_STATES.contains(StateType.valueOf(getStateType()));
  }

  @Attributes(required = true, title = "Include nodes from previous phases")
  public boolean getIncludePreviousPhaseNodes() {
    return includePreviousPhaseNodes;
  }

  public void setIncludePreviousPhaseNodes(boolean includePreviousPhaseNodes) {
    this.includePreviousPhaseNodes = includePreviousPhaseNodes;
  }

  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  public void setComparisonStrategy(String comparisonStrategy) {
    this.comparisonStrategy = comparisonStrategy;
  }

  @Attributes(title = "Predictive history in Minutes")
  @DefaultValue("30")
  public String getPredictiveHistoryMinutes() {
    if (isEmpty(predictiveHistoryMinutes)) {
      return String.valueOf(PREDECTIVE_HISTORY_MINUTES);
    }
    return predictiveHistoryMinutes;
  }

  public void setPredictiveHistoryMinutes(String predictiveHistoryMinutes) {
    this.predictiveHistoryMinutes = predictiveHistoryMinutes;
  }

  public AbstractAnalysisState(String name, String stateType) {
    super(name, stateType);
  }
  protected void saveMetaDataForDashboard(String accountId, ExecutionContext executionContext) {
    try {
      WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(
          executionContext.getAppId(), executionContext.getWorkflowExecutionId());

      // TODO: ASR: update this for multi-artifact
      final Artifact artifactForService =
          ((ExecutionContextImpl) executionContext).getArtifactForService(getPhaseServiceId(executionContext));
      ContinuousVerificationExecutionMetaDataBuilder cvExecutionMetaDataBuilder =
          ContinuousVerificationExecutionMetaData.builder()
              .accountId(accountId)
              .applicationId(executionContext.getAppId())
              .workflowExecutionId(executionContext.getWorkflowExecutionId())
              .workflowId(executionContext.getWorkflowId())
              .stateExecutionId(executionContext.getStateExecutionInstanceId())
              .serviceId(getPhaseServiceId(executionContext))
              .envName(getEnvName(executionContext))
              .workflowName(executionContext.getWorkflowExecutionName())
              .appName(getAppName(executionContext))
              .artifactName(artifactForService == null ? null : artifactForService.getDisplayName())
              .serviceName(getPhaseServiceName(executionContext))
              .workflowStartTs(workflowExecution.getStartTs())
              .stateType(StateType.valueOf(getStateType()))
              .stateStartTs(((ExecutionContextImpl) executionContext).getStateExecutionInstance().getStartTs())
              .phaseName(getPhaseName(executionContext))
              .phaseId(getPhaseId(executionContext))
              .executionStatus(ExecutionStatus.RUNNING)
              .envId(getEnvId(executionContext));

      if (workflowExecution.getPipelineExecutionId() != null) {
        WorkflowExecution pipelineExecutionDetails = workflowExecutionService.getExecutionDetails(
            executionContext.getAppId(), workflowExecution.getPipelineExecutionId(), false);
        cvExecutionMetaDataBuilder.pipelineName(pipelineExecutionDetails.normalizedName())
            .pipelineStartTs(pipelineExecutionDetails.getStartTs())
            .pipelineExecutionId(workflowExecution.getPipelineExecutionId())
            .pipelineId(workflowExecution.getExecutionArgs().getPipelineId());
      }

      ContinuousVerificationExecutionMetaData metaData = cvExecutionMetaDataBuilder.build();
      metaData.setAppId(executionContext.getAppId());
      continuousVerificationService.saveCVExecutionMetaData(metaData);
    } catch (Exception ex) {
      getLogger().error("[learning-engine] Unable to save ml analysis metadata", ex);
    }
  }

  protected String scheduleAnalysisCronJob(AnalysisContext context, String delegateTaskId) {
    context.setDelegateTaskId(delegateTaskId);
    return wingsPersistence.save(context);
  }

  protected Map<String, String> getLastExecutionNodes(ExecutionContext context) {
    String serviceId = getPhaseServiceId(context);
    Service service = serviceResourceService.get(context.getAppId(), serviceId, false);
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    String infraMappingId = infrastructureMapping.getUuid();
    if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      return getPcfHostNames(context, true);
    }

    if (infrastructureMapping instanceof AwsAmiInfrastructureMapping) {
      return getAwsAmiHostNames(context, (AwsAmiInfrastructureMapping) infrastructureMapping);
    }

    DeploymentType deploymentType = serviceResourceService.getDeploymentType(infrastructureMapping, null, serviceId);
    Map<String, String> phaseHosts = getHostsDeployedSoFar(context, serviceId, deploymentType);
    getLogger().info("Deployed hosts so far: {}", phaseHosts);

    if (service.isK8sV2()) {
      getLogger().info(
          "For {}, executionContext in getLastExecutionNodes is {}", context.getStateExecutionInstanceId(), context);
      final Map<String, String> hosts = new HashMap<>();
      K8sElement k8sElement = k8sStateHelper.getK8sElement(context);
      if (k8sElement != null) {
        ContainerInfrastructureMapping containerInfrastructureMapping =
            (ContainerInfrastructureMapping) infrastructureMapping;
        getLogger().info(
            "For {} Calling tryGetPodList with containerInfrastructureMapping : {}, Namespace: {}, releaseName: {}",
            context.getStateExecutionInstanceId(), containerInfrastructureMapping,
            containerInfrastructureMapping.getNamespace(), k8sElement.getReleaseName());
        List<K8sPod> currentPods = k8sStateHelper.tryGetPodList(
            containerInfrastructureMapping, containerInfrastructureMapping.getNamespace(), k8sElement.getReleaseName());
        getLogger().info(
            "For {}, Current Pods returned in K8sV2 are {}", context.getStateExecutionInstanceId(), currentPods);
        if (currentPods != null) {
          currentPods.forEach(pod -> {
            if (isEmpty(hostnameTemplate)) {
              hosts.put(pod.getName(), DEFAULT_GROUP_NAME);
            } else {
              hosts.put(context.renderExpression(hostnameTemplate,
                            StateExecutionContext.builder()
                                .contextElements(Lists.newArrayList(
                                    HostElement.builder().hostName(pod.getName()).ip(pod.getPodIP()).build()))
                                .build()),
                  DEFAULT_GROUP_NAME);
            }
          });
          phaseHosts.keySet().forEach(hosts::remove);
        }
      }
      getLogger().info("hosts returned as getLastExecutionNodes in K8sV2 for {} are {}",
          context.getStateExecutionInstanceId(), hosts);
      return hosts;
    } else if (containerInstanceHandler.isContainerDeployment(infrastructureMapping)) {
      Set<ContainerMetadata> containerMetadataSet =
          containerInstanceHandler.getContainerServiceNames(context, serviceId, infraMappingId,
              isEmpty(infrastructureMapping.getInfrastructureDefinitionId())
                  ? Optional.empty()
                  : Optional.of(infrastructureMapping.getInfrastructureDefinitionId()));

      if (isEmpty(containerMetadataSet)) {
        getLogger().info("state {} has no containers deployed for service {} infra {}. Returning empty",
            context.getStateExecutionInstanceId(), serviceId, infraMappingId);
        return Collections.emptyMap();
      }

      if (infrastructureMapping instanceof EcsInfrastructureMapping) {
        Map<String, String> hosts = getEcsLastExecutionNodes(context, (EcsInfrastructureMapping) infrastructureMapping,
            containerMetadataSet.stream().map(ContainerMetadata::getContainerServiceName).collect(toSet()));
        phaseHosts.keySet().forEach(hosts::remove);
        return hosts;
      }

      List<ContainerInfo> containerInfoForService =
          containerInstanceHandler.getContainerInfoForService(containerMetadataSet, context, infraMappingId, serviceId);
      Map<String, String> podNameToIp = new HashMap<>();
      Set<String> serviceHosts = containerInfoForService.stream()
                                     .map(containerInfo -> {
                                       if (containerInfo instanceof KubernetesContainerInfo) {
                                         String podName = ((KubernetesContainerInfo) containerInfo).getPodName();
                                         podNameToIp.put(podName, ((KubernetesContainerInfo) containerInfo).getIp());
                                         return podName;
                                       }

                                       if (containerInfo instanceof EcsContainerInfo) {
                                         return ((EcsContainerInfo) containerInfo).getServiceName();
                                       }

                                       throw new IllegalStateException("Invalid type " + containerInfo);
                                     })
                                     .collect(toSet());
      final Map<String, String> hosts = new HashMap<>();
      serviceHosts.forEach(serviceHost -> {
        if (isEmpty(hostnameTemplate)) {
          hosts.put(serviceHost, DEFAULT_GROUP_NAME);
        } else {
          hosts.put(
              context.renderExpression(hostnameTemplate,
                  StateExecutionContext.builder()
                      .contextElements(Lists.newArrayList(
                          anInstanceElement()
                              .hostName(serviceHost)
                              .host(
                                  HostElement.builder().hostName(serviceHost).ip(podNameToIp.get(serviceHost)).build())
                              .build()))
                      .build()),
              DEFAULT_GROUP_NAME);
        }
      });
      phaseHosts.keySet().forEach(hosts::remove);
      return hosts;
    }

    String envId = getEnvId(context);
    String phaseInfraMappingId = getPhaseInfraMappingId(context);

    List<WorkflowExecution> successfulExecutions = new ArrayList<>();
    List<WorkflowExecution> executions = workflowExecutionService.getLastSuccessfulWorkflowExecutions(
        context.getAppId(), getWorkflowId(context), service.getUuid());

    // Filter the list of executions by the correct infra mapping ID also.
    for (WorkflowExecution execution : executions) {
      if (execution.getInfraMappingIds().contains(phaseInfraMappingId) && execution.getEnvId().equals(envId)) {
        getLogger().info("Execution {} matches infraMappingID {} or envId {}. So adding to successful list.",
            execution.getUuid(), infraMappingId, envId);
        successfulExecutions.add(execution);
      }
    }

    if (isEmpty(successfulExecutions)) {
      getLogger().info("Did not find a successful workflow with service {}. It will be a baseline run", serviceId);
      return emptyMap();
    }

    for (WorkflowExecution workflowExecution : successfulExecutions) {
      ElementExecutionSummary executionSummary = null;
      for (ElementExecutionSummary summary : workflowExecution.getServiceExecutionSummaries()) {
        if (summary.getContextElement().getUuid().equals(serviceId)) {
          executionSummary = summary;
          break;
        }
      }

      if (executionSummary != null) {
        if (isEmpty(executionSummary.getInstanceStatusSummaries())) {
          return emptyMap();
        }
        Map<String, String> hosts = new HashMap<>();
        for (InstanceStatusSummary instanceStatusSummary : executionSummary.getInstanceStatusSummaries()) {
          if (isEmpty(getHostnameTemplate())) {
            hosts.put(instanceStatusSummary.getInstanceElement().getHostName(), DEFAULT_GROUP_NAME);
          } else {
            fillHostDetail(instanceStatusSummary.getInstanceElement(), context);
            hosts.put(context.renderExpression(getHostnameTemplate(),
                          StateExecutionContext.builder()
                              .contextElements(Lists.newArrayList(instanceStatusSummary.getInstanceElement()))
                              .build()),
                DEFAULT_GROUP_NAME);
          }
        }
        getLogger().info("hosts deployed with last workflow execution: {}", hosts);
        phaseHosts.keySet().forEach(hosts::remove);
        return hosts;
      }
    }

    getLogger().info("Did not find a successful workflow with service {}. It will be a baseline run", serviceId);
    return emptyMap();
  }

  private Map<String, String> getEcsLastExecutionNodes(ExecutionContext context,
      EcsInfrastructureMapping containerInfrastructureMapping, Set<String> containerServiceNames) {
    String clusterName = containerInfrastructureMapping.getClusterName();
    String region = containerInfrastructureMapping.getRegion();
    SettingAttribute settingAttribute =
        getSettingAttribute(containerInfrastructureMapping.getComputeProviderSettingId());
    Preconditions.checkNotNull(settingAttribute.getValue(), "Cloud provider setting not found for " + settingAttribute);
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());
    String accountId = this.appService.get(context.getAppId()).getAccountId();
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder()
            .accountId(accountId)
            .appId(context.getAppId())
            .envId(containerInfrastructureMapping.getEnvId())
            .infrastructureMappingId(containerInfrastructureMapping.getUuid())
            .infraStructureDefinitionId(containerInfrastructureMapping.getInfrastructureDefinitionId())
            .timeout(DEFAULT_SYNC_CALL_TIMEOUT * 2)
            .build();
    List<software.wings.cloudprovider.ContainerInfo> containerInfos =
        delegateProxyFactory.get(ContainerService.class, syncTaskContext)
            .fetchContainerInfos(ContainerServiceParams.builder()
                                     .containerServiceNames(containerServiceNames)
                                     .clusterName(clusterName)
                                     .region(region)
                                     .encryptionDetails(encryptionDetails)
                                     .settingAttribute(settingAttribute)
                                     .build());

    Map<String, String> map = new HashMap<>();
    getLogger().info("Container info list : " + containerInfos);
    containerInfos.forEach(containerInfo
        -> Preconditions.checkState(containerInfo.isContainerTasksReachable(),
            "Could not reach ECS to determine container ids. Please check execution logs of deployment and make sure the delegate has connectivity to cluster resources."));

    for (software.wings.cloudprovider.ContainerInfo containerInfo : containerInfos) {
      String evaluatedHost = isEmpty(getHostnameTemplate())
          ? containerInfo.getContainerId()
          : context.renderExpression(getHostnameTemplate(),
                StateExecutionContext.builder()
                    .contextElements(Lists.newArrayList(anInstanceElement()
                                                            .hostName(containerInfo.getHostName())
                                                            .podName(containerInfo.getPodName())
                                                            .workloadName(containerInfo.getWorkloadName())
                                                            .host(HostElement.builder()
                                                                      .hostName(containerInfo.getHostName())
                                                                      .ip(containerInfo.getIp())
                                                                      .ec2Instance(containerInfo.getEc2Instance())
                                                                      .build())
                                                            .ecsContainerDetails(containerInfo.getEcsContainerDetails())
                                                            .build()))
                    .build());
      map.put(evaluatedHost, DEFAULT_GROUP_NAME);
    }
    getLogger().info("map created : " + map);
    return map;
  }

  private Map<String, String> getHostsDeployedSoFar(
      ExecutionContext context, String serviceId, DeploymentType deploymentType) {
    Map<String, String> hosts = new HashMap<>();
    StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();
    Preconditions.checkNotNull(stateExecutionInstance);

    PhaseElement contextPhaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    List<StateExecutionData> stateExecutionDataList = stateExecutionService.fetchPhaseExecutionData(
        stateExecutionInstance.getAppId(), stateExecutionInstance.getExecutionUuid(),
        contextPhaseElement == null ? null : contextPhaseElement.getPhaseName(), CurrentPhase.INCLUDE);

    if (isEmpty(stateExecutionDataList)) {
      return hosts;
    }

    stateExecutionDataList.forEach(stateExecutionData -> {
      PhaseExecutionData phaseExecutionData = (PhaseExecutionData) stateExecutionData;
      phaseExecutionData.getElementStatusSummary().forEach(elementExecutionSummary -> {
        PhaseElement phaseElement = (PhaseElement) elementExecutionSummary.getContextElement();
        if (phaseElement == null) {
          getLogger().error("null phase element for " + elementExecutionSummary);
          return;
        }

        ServiceElement serviceElement = phaseElement.getServiceElement();
        if (serviceElement == null) {
          getLogger().error("null service element for " + phaseElement);
          return;
        }

        if (serviceElement.getUuid().equals(serviceId)) {
          if (isNotEmpty(elementExecutionSummary.getInstanceStatusSummaries())) {
            elementExecutionSummary.getInstanceStatusSummaries().forEach(instanceStatusSummary -> {
              String hostName;
              if (isEmpty(getHostnameTemplate())) {
                hostName = instanceStatusSummary.getInstanceElement().getHostName();
              } else {
                fillHostDetail(instanceStatusSummary.getInstanceElement(), context);
                hostName = context.renderExpression(getHostnameTemplate(),
                    StateExecutionContext.builder()
                        .contextElements(Lists.newArrayList(instanceStatusSummary.getInstanceElement()))
                        .build());
              }
              if (!isNewInstanceFieldPopulated(context) || instanceStatusSummary.getInstanceElement().isNewInstance()) {
                hosts.put(hostName, getGroupName(instanceStatusSummary.getInstanceElement(), deploymentType));
              }
            });
          } else {
            getLogger().warn(
                "elementExecutionSummary does not have instance summaries. This may lead to incorrect canary analysis. {}",
                elementExecutionSummary);
          }
        }
      });
    });
    getLogger().info("Hosts deployed so far for {} are {}", context.getStateExecutionInstanceId(), hosts);
    return hosts;
  }

  protected boolean isNewInstanceFieldPopulated(ExecutionContext context) {
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    return infrastructureMapping instanceof AwsAmiInfrastructureMapping
        && AmiDeploymentType.SPOTINST == ((AwsAmiInfrastructureMapping) infrastructureMapping).getAmiDeploymentType();
  }

  protected void populateNewAndOldHostNames(
      ExecutionContext context, Map<String, String> controlNodes, Map<String, String> testNodes) {
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    if (includePreviousPhaseNodes) {
      testNodes.putAll(getHostsDeployedSoFar(context, getPhaseServiceId(context), getDeploymentType(context)));
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Preconditions.checkArgument(isNotEmpty(workflowStandardParams.getInstances()));
    for (InstanceElement instanceElement : workflowStandardParams.getInstances()) {
      DeploymentType deploymentType =
          serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());

      String hostName = isEmpty(getHostnameTemplate())
          ? instanceElement.getHostName()
          : context.renderExpression(getHostnameTemplate(),
                StateExecutionContext.builder().contextElements(Lists.newArrayList(instanceElement)).build());
      if (instanceElement.isNewInstance()) {
        testNodes.put(hostName, getGroupName(instanceElement, deploymentType));
        continue;
      }
      if (getComparisonStrategy() != COMPARE_WITH_PREVIOUS) {
        controlNodes.put(hostName, getGroupName(instanceElement, deploymentType));
      }
    }
  }

  protected Map<String, String> getCanaryNewHostNames(ExecutionContext context) {
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);

    if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      return getPcfHostNames(context, false);
    }

    Map<String, String> rv = new HashMap<>();
    if (includePreviousPhaseNodes) {
      getLogger().info("returning all phases nodes for state {}", context.getStateExecutionInstanceId());
      rv.putAll(getHostsDeployedSoFar(context, getPhaseServiceId(context), getDeploymentType(context)));
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (isAwsLambdaState(context) || isEmpty(workflowStandardParams.getInstances())) {
      getLogger().warn(
          "No test nodes found for state: {}, id: {} ", getStateType(), context.getStateExecutionInstanceId());
      return rv;
    }
    for (InstanceElement instanceElement : workflowStandardParams.getInstances()) {
      DeploymentType deploymentType =
          serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());

      if (isEmpty(getHostnameTemplate())) {
        rv.put(instanceElement.getHostName(), getGroupName(instanceElement, deploymentType));
      } else {
        fillHostDetail(instanceElement, context);
        rv.put(context.renderExpression(getHostnameTemplate(),
                   StateExecutionContext.builder().contextElements(Lists.newArrayList(instanceElement)).build()),
            getGroupName(instanceElement, deploymentType));
      }
    }
    return rv;
  }

  private Map<String, String> getPcfHostNames(ExecutionContext context, boolean includePrevious) {
    StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();
    LinkedList<ContextElement> contextElements = stateExecutionInstance.getContextElements();
    Map<String, String> rv = new HashMap<>();
    if (isEmpty(contextElements)) {
      return rv;
    }

    contextElements.forEach(contextElement -> {
      if (contextElement instanceof InstanceElementListParam) {
        InstanceElementListParam instances = (InstanceElementListParam) contextElement;
        instances.getPcfInstanceElements().forEach(pcfInstanceElement -> {
          String pcfHostName = getPcfHostName(pcfInstanceElement, includePrevious);
          addToRvMap(context, rv, pcfInstanceElement, pcfHostName);
        });

        if (includePrevious && isNotEmpty(instances.getPcfOldInstanceElements())) {
          instances.getPcfOldInstanceElements().forEach(pcfInstanceElement -> {
            String pcfHostName = getPcfHostName(pcfInstanceElement, includePrevious);
            addToRvMap(context, rv, pcfInstanceElement, pcfHostName);
          });
        }
      }
    });
    return rv;
  }

  private void addToRvMap(
      ExecutionContext context, Map<String, String> rv, PcfInstanceElement pcfInstanceElement, String pcfHostName) {
    if (isNotEmpty(pcfHostName)) {
      if (isEmpty(hostnameTemplate)) {
        rv.put(pcfHostName, DEFAULT_GROUP_NAME);
      } else {
        rv.put(context.renderExpression(hostnameTemplate,
                   StateExecutionContext.builder()
                       .contextElements(Lists.newArrayList(
                           HostElement.builder().hostName(pcfHostName).pcfElement(pcfInstanceElement).build()))
                       .build()),
            DEFAULT_GROUP_NAME);
      }
    }
  }

  private Map<String, String> getAwsAmiHostNames(
      ExecutionContext context, AwsAmiInfrastructureMapping infrastructureMapping) {
    Map<String, String> hosts = new HashMap<>();
    final String providerSettingId = infrastructureMapping.getComputeProviderSettingId();
    final SettingAttribute settingAttribute = getSettingAttribute(providerSettingId);
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    Preconditions.checkNotNull(awsConfig, "No aws config set for " + providerSettingId);
    AmiServiceSetupElement serviceSetupElement = context.getContextElement(ContextElementType.AMI_SERVICE_SETUP);
    if (serviceSetupElement == null) {
      throw new VerificationOperationException(ErrorCode.APM_CONFIGURATION_ERROR,
          "Could not find service element for  " + context.getStateExecutionInstanceId());
    }
    if (serviceSetupElement.getPreDeploymentData() == null) {
      return hosts;
    }
    Set<String> asgNames = new HashSet<>();
    if (isNotEmpty(serviceSetupElement.getPreDeploymentData().getOldAsgName())) {
      asgNames.add(serviceSetupElement.getPreDeploymentData().getOldAsgName());
    }
    getLogger().info("For {} going to fetch instance from asg {}", context.getStateExecutionInstanceId(), asgNames);

    asgNames.forEach(asgName -> {
      final List<Instance> instances = awsAsgHelperServiceManager.listAutoScalingGroupInstances(awsConfig,
          secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()),
          infrastructureMapping.getRegion(), asgName, context.getAppId());

      if (isNotEmpty(instances)) {
        instances.forEach(instance -> {
          String hostNameExpression =
              isNotEmpty(hostnameTemplate) ? hostnameTemplate : infrastructureMapping.getHostNameConvention();
          if (isEmpty(hostNameExpression)) {
            hosts.put(instance.getPrivateDnsName(), DEFAULT_GROUP_NAME);
          } else {
            hosts.put(context.renderExpression(hostNameExpression,
                          StateExecutionContext.builder()
                              .contextElements(Lists.newArrayList(HostElement.builder()
                                                                      .hostName(instance.getPrivateDnsName())
                                                                      .ip(instance.getPrivateIpAddress())
                                                                      .ec2Instance(instance)
                                                                      .build()))
                              .build()),
                DEFAULT_GROUP_NAME);
          }
        });
      }
    });
    return hosts;
  }

  protected boolean isAwsLambdaState(ExecutionContext context) {
    return getStateType().equals(StateType.CLOUD_WATCH.name())
        && getDeploymentType(context) == DeploymentType.AWS_LAMBDA;
  }

  protected boolean isAwsECSState(ExecutionContext context) {
    return getStateType().equals(StateType.CLOUD_WATCH.name()) && getDeploymentType(context) == DeploymentType.ECS;
  }

  protected String getPcfHostName(PcfInstanceElement pcfInstanceElement, boolean includePrevious) {
    if ((includePrevious && !pcfInstanceElement.isUpsize()) || (!includePrevious && pcfInstanceElement.isUpsize())) {
      return pcfInstanceElement.getDisplayName() + ":" + pcfInstanceElement.getInstanceIndex();
    }

    return null;
  }

  PhaseElement getPhaseElement(ExecutionContext context) {
    return context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
  }
  protected String getPhaseServiceId(ExecutionContext context) {
    PhaseElement phaseElement = getPhaseElement(context);
    return phaseElement.getServiceElement().getUuid();
  }

  protected String getPhaseInfraMappingId(ExecutionContext context) {
    return context.fetchInfraMappingId();
  }

  protected DeploymentType getDeploymentType(ExecutionContext context) {
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    return serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());
  }

  protected String getPhaseServiceName(ExecutionContext context) {
    PhaseElement phaseElement = getPhaseElement(context);
    return phaseElement.getServiceElement().getName();
  }

  protected String getPhaseName(ExecutionContext context) {
    PhaseElement phaseElement = getPhaseElement(context);
    return phaseElement.getPhaseName();
  }

  protected String getWorkflowId(ExecutionContext context) {
    final WorkflowExecution executionDetails =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    return executionDetails.getWorkflowId();
  }

  protected String getPhaseId(ExecutionContext context) {
    PhaseElement phaseElement = getPhaseElement(context);
    return phaseElement.getUuid();
  }

  @SchemaIgnore public abstract Logger getLogger();

  public abstract String getAnalysisServerConfigId();

  public abstract void setAnalysisServerConfigId(String analysisServerConfigId);

  protected void generateDemoActivityLogs(CVActivityLogService.Logger activityLogger, boolean failedState) {
    logDataCollectionTriggeredMessage(activityLogger);
    long startTime = dataCollectionStartTimestampMillis();
    int duration = Integer.parseInt(getTimeDuration());
    for (int minute = 0; minute < duration; minute++) {
      long startTimeMSForCurrentMinute = startTime + Duration.ofMinutes(minute).toMillis();
      long endTimeMSForCurrentMinute = startTimeMSForCurrentMinute + Duration.ofMinutes(1).toMillis();
      activityLogger.info(
          "Starting data collection. Time range %t to %t", startTimeMSForCurrentMinute, endTimeMSForCurrentMinute);
      if (minute == duration - 1 && failedState) {
        activityLogger.info(
            "Starting data collection. Time range %t to %t", startTimeMSForCurrentMinute, endTimeMSForCurrentMinute);
        activityLogger.error(
            "Data collection failed for time range %t to %t", startTimeMSForCurrentMinute, endTimeMSForCurrentMinute);
      } else {
        activityLogger.info("Data collection successful for time range %t to %t", startTimeMSForCurrentMinute,
            endTimeMSForCurrentMinute);
        activityLogger.info(
            "Analysis completed for time range %t to %t", startTimeMSForCurrentMinute, endTimeMSForCurrentMinute);
      }
    }
    if (failedState) {
      activityLogger.error("Analysis failed");
    } else {
      activityLogger.info("Analysis successful");
    }
  }

  protected boolean isDemoPath(AnalysisContext analysisContext) {
    return featureFlagService.isEnabled(FeatureName.CV_DEMO, analysisContext.getAccountId())
        && (getSettingAttribute(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev")
               || getSettingAttribute(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("prod"));
  }

  protected void generateDemoThirdPartyApiCallLogs(
      String accountId, String stateExecutionId, boolean failedState, String demoRequestBody, String demoResponseBody) {
    List<ThirdPartyApiCallLog> thirdPartyApiCallLogs = new ArrayList<>();
    long startTime = dataCollectionStartTimestampMillis();
    int duration = Integer.parseInt(getTimeDuration());
    for (int minute = 0; minute < duration; minute++) {
      long startTimeMSForCurrentMinute = startTime + Duration.ofMinutes(minute).toMillis();
      ThirdPartyApiCallLog apiCallLog = ThirdPartyApiCallLog.createApiCallLog(accountId, stateExecutionId);
      apiCallLog.setTitle("Demo third party API call log");
      apiCallLog.setRequestTimeStamp(startTimeMSForCurrentMinute);
      apiCallLog.addFieldToRequest(
          ThirdPartyApiCallField.builder().name(URL_STRING).value("http://example.com/").type(FieldType.URL).build());
      apiCallLog.addFieldToRequest(
          ThirdPartyApiCallField.builder().name(PAYLOAD).value(demoRequestBody).type(FieldType.JSON).build());
      if (minute == duration / 2 && failedState) {
        apiCallLog.addFieldToResponse(408, "Timeout from service provider", FieldType.JSON);
      } else {
        apiCallLog.addFieldToResponse(200, demoResponseBody, FieldType.JSON);
      }
      apiCallLog.setResponseTimeStamp(startTimeMSForCurrentMinute + random.nextInt(100));
      thirdPartyApiCallLogs.add(apiCallLog);
    }
    wingsPersistence.save(thirdPartyApiCallLogs);
  }

  /**
   *  for QA in harness verification app, fail if there is no data but don't fail for anomaly
   */
  protected boolean isQAVerificationPath(String accountId, String appId) {
    return featureFlagService.isEnabled(CV_SUCCEED_FOR_ANOMALY, accountId)
        && appService.get(appId).getName().equals("Harness Verification");
  }

  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  private void fillHostDetail(InstanceElement instanceElement, ExecutionContext context) {
    Preconditions.checkNotNull(instanceElement);
    HostElement hostElement = instanceElement.getHost();
    if (hostElement == null || isEmpty(hostElement.getUuid())) {
      getLogger().info("for {} no host element set for {}", context.getStateExecutionInstanceId(), instanceElement);
      return;
    }
    Host host = hostService.get(context.getAppId(), getEnvId(context), hostElement.getUuid());
    if (host == null) {
      getLogger().info("for {} no host found for {}", context.getStateExecutionInstanceId(), hostElement);
      return;
    }
    getLogger().info("For {}, setting hostName as {}, ec2Instance as {} and publicDns as {}",
        context.getStateExecutionInstanceId(), host.getHostName(), host.getEc2Instance(), host.getPublicDns());
    instanceElement.setHost(HostElement.builder()
                                .hostName(host.getHostName())
                                .ec2Instance(host.getEc2Instance())
                                .publicDns(host.getPublicDns())
                                .build());
  }

  @Override
  @SchemaIgnore
  public Integer getTimeoutMillis() {
    if (!isEmpty(timeDuration)) {
      return 60 * 1000 * (Integer.parseInt(timeDuration) + TIMEOUT_BUFFER);
    }
    return DEFAULT_VERIFICATION_STATE_TIMEOUT_MILLIS;
  }

  @Override
  public Map<String, String> validateFields() {
    if (!isEmpty(timeDuration) && Integer.parseInt(timeDuration) > MAX_WORKFLOW_TIMEOUT) {
      return new HashMap<String, String>() {
        { put("timeDuration", "Time duration cannot be more than 4 hours."); }
      };
    }
    return null;
  }

  protected InfrastructureMapping getInfrastructureMapping(ExecutionContext context) {
    String infraMappingId = context.fetchInfraMappingId();
    Preconditions.checkNotNull(
        context.fetchInfraMappingId(), "for " + context.getStateExecutionInstanceId() + " no infra mapping id found");
    return infraMappingService.get(context.getAppId(), infraMappingId);
  }

  private String getGroupName(InstanceElement instanceElement, DeploymentType deploymentType) {
    if (deploymentType == null || deploymentType != DeploymentType.HELM || isEmpty(instanceElement.getWorkloadName())) {
      return DEFAULT_GROUP_NAME;
    }

    return instanceElement.getWorkloadName();
  }

  protected boolean checkLicense(String accountId, StateType stateType, String stateExecutionId) {
    switch (stateType) {
      case PROMETHEUS:
      case STACK_DRIVER:
      case STACK_DRIVER_LOG:
      case CLOUD_WATCH:
        return true;
      default:
        noop();
    }

    final Optional<String> accountType = accountService.getAccountType(accountId);
    if (!accountType.isPresent()) {
      getLogger().info("for id {} and stateType {} the account has no type set. License check will not be enforced",
          stateExecutionId, stateType);
      return false;
    }
    if (COMMUNITY.equals(accountType.get()) || ESSENTIALS.equals(accountType.get())) {
      getLogger().info("for id {}, stateType {} the account type {} does not have license to run. Skipping analysis",
          stateExecutionId, stateType, accountType.get());
      return false;
    }

    return true;
  }

  protected boolean unresolvedHosts(AnalysisContext analysisContext) {
    return unresolvedHosts(analysisContext.getTestNodes()) || unresolvedHosts(analysisContext.getControlNodes());
  }

  private boolean unresolvedHosts(Map<String, String> hostToGroupMap) {
    return hostToGroupMap.entrySet().stream().anyMatch(entry -> {
      String hostName = entry.getKey();
      if (hostName.contains("${") && hostName.contains("}")) {
        getLogger().info("{} is not resolved", hostName);
        return true;
      }
      return false;
    });
  }

  protected long dataCollectionStartTimestampMillis() {
    return Timestamp.currentMinuteBoundary();
  }

  private long dataCollectionEndTimestampMillis(long dataCollectionStartTime) {
    return Instant.ofEpochMilli(dataCollectionStartTime)
        .plusMillis(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration())))
        .toEpochMilli();
  }

  protected void logDataCollectionTriggeredMessage(CVActivityLogService.Logger activityLogger) {
    long dataCollectionStartTime = dataCollectionStartTimestampMillis();
    long initDelayMins = TimeUnit.SECONDS.toMinutes(getDelaySeconds(initialAnalysisDelay));
    activityLogger.info("Triggered data collection for " + getTimeDuration()
            + " minutes, Data will be collected for time range %t to %t. Waiting for " + initDelayMins
            + " minutes before starting data collection.",
        dataCollectionStartTime, dataCollectionEndTimestampMillis(dataCollectionStartTime));
  }

  protected boolean isCVTaskEnqueuingEnabled(String accountId) {
    Optional<FeatureName> cvTaskFeatureName = getCVTaskFeatureName();
    if (cvTaskFeatureName.isPresent()) {
      return featureFlagService.isEnabled(cvTaskFeatureName.get(), accountId);
    } else {
      return false;
    }
  }

  protected Optional<FeatureName> getCVTaskFeatureName() {
    return Optional.empty();
  }

  protected boolean isExpression(String fieldName, String fieldValue, List<TemplateExpression> templateExpressions) {
    if (checkFieldTemplatized(fieldName, templateExpressions)) {
      return false;
    }
    if (isEmpty(fieldValue)) {
      return false;
    }

    return fieldValue.contains("$");
  }

  protected ExecutionResponse createExecutionResponse(
      AnalysisContext context, ExecutionStatus status, String message, boolean updateCVMetadataState) {
    final VerificationStateAnalysisExecutionData executionData =
        VerificationStateAnalysisExecutionData.builder()
            .stateExecutionInstanceId(context.getStateExecutionId())
            .serverConfigId(context.getAnalysisServerConfigId())
            .baselineExecutionId(context.getPrevWorkflowExecutionId())
            .canaryNewHostNames(
                isEmpty(context.getTestNodes()) ? Collections.emptySet() : context.getTestNodes().keySet())
            .lastExecutionNodes(
                isEmpty(context.getControlNodes()) ? Collections.emptySet() : context.getControlNodes().keySet())
            .correlationId(context.getCorrelationId())
            .query(context.getQuery())
            .customThresholdRefId(context.getCustomThresholdRefId())
            .comparisonStrategy(getComparisonStrategy())
            .build();
    executionData.setStatus(status);
    if (updateCVMetadataState) {
      continuousVerificationService.setMetaDataExecutionStatus(context.getStateExecutionId(), status, true, false);
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionId())
          .info(message);
    }
    return ExecutionResponse.builder()
        .async(false)
        .executionStatus(status)
        .stateExecutionData(executionData)
        .errorMessage(message)
        .build();
  }

  protected ExecutionResponse getDemoExecutionResponse(AnalysisContext analysisContext) {
    boolean failedState = getSettingAttribute(getAnalysisServerConfigId()).getName().toLowerCase().endsWith("dev");
    if (failedState) {
      return generateAnalysisResponse(analysisContext, ExecutionStatus.FAILED, "Demo CV");
    } else {
      return generateAnalysisResponse(analysisContext, ExecutionStatus.SUCCESS, "Demo CV");
    }
  }

  protected abstract ExecutionResponse generateAnalysisResponse(
      AnalysisContext context, ExecutionStatus status, String message);

  protected ExecutionResponse getErrorExecutionResponse(
      ExecutionContext executionContext, VerificationDataAnalysisResponse executionResponse) {
    getLogger().info(
        "for {} got failed execution response {}", executionContext.getStateExecutionInstanceId(), executionResponse);
    continuousVerificationService.setMetaDataExecutionStatus(
        executionContext.getStateExecutionInstanceId(), executionResponse.getExecutionStatus(), true, false);
    return ExecutionResponse.builder()
        .executionStatus(executionResponse.getExecutionStatus())
        .stateExecutionData(executionResponse.getStateExecutionData())
        .errorMessage(executionResponse.getStateExecutionData().getErrorMsg())
        .build();
  }

  protected String getEnvId(ExecutionContext executionContext) {
    return ((ExecutionContextImpl) executionContext).getEnv() == null
        ? null
        : ((ExecutionContextImpl) executionContext).getEnv().getUuid();
  }

  protected String getEnvName(ExecutionContext executionContext) {
    return ((ExecutionContextImpl) executionContext).getEnv() == null
        ? null
        : ((ExecutionContextImpl) executionContext).getEnv().getName();
  }

  protected String getAppName(ExecutionContext executionContext) {
    return executionContext.getApp() == null ? null : executionContext.getApp().getName();
  }

  protected int getDelaySeconds(String initialDelay) {
    // https://harness.atlassian.net/browse/CV-3902
    // this is a hack because delay second is getting removed from UI and we will decide if we want to get rid this
    // logic completely
    return (int) TimeUnit.MINUTES.toSeconds(DELAY_MINUTES);
  }

  protected void sampleHostsMap(AnalysisContext analysisContext) {
    if (featureFlagService.isEnabled(FeatureName.CV_HOST_SAMPLING, analysisContext.getAccountId())) {
      analysisContext.setControlNodes(sampleHostsMap(analysisContext.getControlNodes(), MAX_SAMPLING_SIZE_PER_GROUP));
      analysisContext.setTestNodes(sampleHostsMap(analysisContext.getTestNodes(), MAX_SAMPLING_SIZE_PER_GROUP));
    }
  }

  private Map<String, String> sampleHostsMap(Map<String, String> hostToGroupMap, int maxSizePerGroup) {
    Map<String, List<String>> groupToHostMap = new HashMap<>();
    for (Map.Entry<String, String> entry : hostToGroupMap.entrySet()) {
      List<String> list = groupToHostMap.getOrDefault(entry.getValue(), new ArrayList<>());
      list.add(entry.getKey());
      groupToHostMap.put(entry.getValue(), list);
    }
    Map<String, String> sampledMap = new HashMap<>();
    for (Map.Entry<String, List<String>> entry : groupToHostMap.entrySet()) {
      String groupName = entry.getKey();
      List<String> hosts = entry.getValue();
      hosts = randomSample(hosts, maxSizePerGroup);
      hosts.forEach(host -> sampledMap.put(host, groupName));
    }
    return sampledMap;
  }

  private List<String> randomSample(List<String> hosts, int maxHosts) {
    Collections.shuffle(hosts);
    return hosts.subList(0, Math.min(maxHosts, hosts.size()));
  }

  protected boolean isHistoricalAnalysis(String accountId) {
    return false;
  }

  protected String getResolvedConnectorId(ExecutionContext context, String fieldName, String fieldValue) {
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), fieldName);
      if (configIdExpression != null) {
        SettingAttribute settingAttribute =
            templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
        return settingAttribute.getUuid();
      }
    }

    if (isExpression(fieldName, fieldValue, getTemplateExpressions())) {
      String analysisServerConfigName = context.renderExpression(fieldValue);
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("Expression " + fieldValue + " resolved to " + analysisServerConfigName);
      final SettingAttribute settingAttribute =
          settingsService.getSettingAttributeByName(context.getAccountId(), analysisServerConfigName);
      if (settingAttribute == null) {
        cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
            .error(
                "The evaluated connector name " + analysisServerConfigName + " did not resolve to a valid connector");
        throw new DataCollectionException("Expression " + fieldValue + " resolved to " + analysisServerConfigName
            + ". There was no connector found with this name.");
      }
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("The evaluated connector name " + analysisServerConfigName
              + " successfully resolved to a valid connector");
      return settingAttribute.getUuid();
    }

    return fieldValue;
  }

  protected String getResolvedFieldValue(ExecutionContext context, String fieldName, String fieldValue) {
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression fieldExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), fieldName);
      if (fieldExpression != null) {
        final String resolveTemplateExpression =
            templateExpressionProcessor.resolveTemplateExpression(context, fieldExpression);
        if (isExpression(fieldName, resolveTemplateExpression, Collections.emptyList())) {
          throw new DataCollectionException("Template expression " + fieldValue + " could not be resolved");
        }

        return resolveTemplateExpression;
      }
    }

    if (isExpression(fieldName, fieldValue, getTemplateExpressions())) {
      final String renderedValue = context.renderExpression(fieldValue);
      cvActivityLogService.getLoggerByStateExecutionId(context.getAccountId(), context.getStateExecutionInstanceId())
          .info("Expression " + fieldValue + " resolved to " + renderedValue);
      if (isExpression(fieldName, renderedValue, getTemplateExpressions())) {
        throw new DataCollectionException("Expression " + fieldValue + " could not be resolved");
      }
      return renderedValue;
    }

    return fieldValue;
  }

  protected SettingAttribute getSettingAttribute(String configId) {
    SettingAttribute settingAttribute = settingsService.get(configId);
    Preconditions.checkNotNull(settingAttribute, "No connector found with id " + configId);
    return settingAttribute;
  }

  protected NodePair getControlAndTestNodes(ExecutionContext context) {
    Set<String> controlNodes, testNodes;
    String hostNameTemplate = isEmpty(getHostnameTemplate()) ? DEFAULT_HOSTNAME_TEMPLATE : getHostnameTemplate();

    if (getComparisonStrategy() == COMPARE_WITH_PREVIOUS) {
      testNodes = new HashSet<>(context.renderExpressionsForInstanceDetails(hostNameTemplate, true));
      controlNodes = new HashSet<>();
    } else {
      Set<String> allNodes =
          new HashSet<>(context.renderExpressionsForInstanceDetailsForWorkflow(hostNameTemplate, false));
      testNodes = includePreviousPhaseNodes
          ? new HashSet<>(context.renderExpressionsForInstanceDetailsForWorkflow(hostNameTemplate, true))
          : new HashSet<>(context.renderExpressionsForInstanceDetails(hostNameTemplate, true));
      Set<String> allPhaseNewNodes =
          new HashSet<>(context.renderExpressionsForInstanceDetailsForWorkflow(hostNameTemplate, true));
      controlNodes = Sets.difference(allNodes, allPhaseNewNodes);
    }
    return NodePair.builder().controlNodes(controlNodes).testNodes(testNodes).build();
  }

  protected void campareAndLogNodesUsingNewInstanceAPI(
      ExecutionContext context, Map<String, String> testNodesMap, Map<String, String> controlNodesMap) {
    try {
      Set<String> testNodes = testNodesMap.keySet();
      Set<String> controlNodes = controlNodesMap.keySet();
      if (Stream.concat(testNodesMap.values().stream(), controlNodesMap.values().stream())
              .anyMatch(value -> !value.equals(DEFAULT_GROUP_NAME))) {
        getLogger().info(
            "[NewInstanceAPI][Error][GroupName] GroupName is not default. testNodeMap {} controlNodeMap {}",
            testNodesMap, controlNodesMap);
      }

      long startTime = System.currentTimeMillis();
      NodePair nodePair = getControlAndTestNodes(context);
      getLogger().info(
          "[NewInstanceAPI] Time taken to get control and test nodes: {} ms", System.currentTimeMillis() - startTime);
      Set<String> newControlNodes = nodePair.getControlNodes(), newTestNodes = nodePair.getTestNodes();
      DeploymentType deploymentType = getDeploymentType(context);
      String message = EMPTY;
      if (isEmpty(newTestNodes)) {
        message = "[EmptyTestNodeError] Empty New Test Nodes";
      } else if (isEmpty(testNodes)) {
        message = "[EmptyTestNodeError] Empty Old Test Nodes";
      }
      if (testNodes.equals(newTestNodes) && controlNodes.equals(newControlNodes)) {
        getLogger().info(
            "[NewInstanceAPI] New nodes and old nodes are equal {}. deploymentType: {} ComparisionStrategy: {} testNodes: {}, controlNodes {}",
            message, deploymentType, getComparisonStrategy(), testNodes, controlNodes);
      } else {
        getLogger().info(
            "[NewInstanceAPI][Error] New nodes and old nodes are not equal {}. deploymentType: {} ComparisionStrategy: {}, "
                + "includePreviousPhaseNodes: {} testNodes: {}, controlNodes {}, newTestNodes {}, newControlNodes {}",
            message, deploymentType, getComparisonStrategy(), includePreviousPhaseNodes, testNodes, controlNodes,
            newTestNodes, newControlNodes);
      }
    } catch (Exception e) {
      // this is the new api supposed to replace the old code. Ignoring any exception so that the existing code is not
      // impacted. this code is printing the comparision
      getLogger().error("[NewInstanceAPI][Error] Exception while calling new instance API", e);
    }
  }
  @Value
  @Builder
  protected static class NodePair {
    private Set<String> controlNodes;
    private Set<String> testNodes;
  }
}

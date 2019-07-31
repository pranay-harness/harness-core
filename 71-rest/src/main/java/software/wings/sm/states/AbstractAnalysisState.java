package software.wings.sm.states;

import static io.harness.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.noop;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.AccountType.COMMUNITY;
import static software.wings.beans.AccountType.ESSENTIALS;
import static software.wings.beans.FeatureName.CV_SUCCEED_FOR_ANOMALY;
import static software.wings.delegatetasks.AbstractDelegateDataCollectionTask.PREDECTIVE_HISTORY_MINUTES;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.ExecutionContextImpl.PHASE_PARAM;
import static software.wings.utils.Misc.replaceDotWithUnicode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.model.K8sPod;
import io.harness.waiter.WaitNotifyEngine;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
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
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerMetadata;
import software.wings.service.impl.ContainerServiceParams;
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
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.k8s.K8sStateHelper;
import software.wings.stencils.DefaultValue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractAnalysisState extends State {
  public static final String START_TIME_PLACE_HOLDER = "$startTime";
  public static final String END_TIME_PLACE_HOLDER = "$endTime";
  public static final String HOST_NAME_PLACE_HOLDER = "$hostName";
  protected String timeDuration;
  protected String comparisonStrategy;
  protected String tolerance;
  protected String predictiveHistoryMinutes;

  private static final int DEFAULT_VERIFICATION_STATE_TIMEOUT_MILLIS = 3 * 60 * 60 * 1000; // 3 hours
  private static final int TIMEOUT_BUFFER = 150; // 150 Minutes.
  private static final int MAX_WORKFLOW_TIMEOUT = 4 * 60; // 4 hours

  @Transient @Inject protected WorkflowExecutionService workflowExecutionService;

  @Transient @Inject protected WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject protected SettingsService settingsService;

  @Inject @Transient protected WingsPersistence wingsPersistence;

  @Transient @Inject protected AppService appService;

  @Transient @Inject protected DelegateService delegateService;

  @Inject @Transient protected SecretManager secretManager;

  @Transient @Inject @SchemaIgnore protected MainConfiguration configuration;

  @Transient @Inject @SchemaIgnore protected ContainerInstanceHandler containerInstanceHandler;

  @Transient @Inject @SchemaIgnore protected InfrastructureMappingService infraMappingService;

  @Transient @Inject protected TemplateExpressionProcessor templateExpressionProcessor;

  @Transient @Inject @SchemaIgnore protected WorkflowExecutionBaselineService workflowExecutionBaselineService;

  @Transient @Inject @SchemaIgnore protected ContinuousVerificationService continuousVerificationService;

  @Transient @Inject @SchemaIgnore private AwsHelperService awsHelperService;

  @Transient @Inject @SchemaIgnore private DelegateProxyFactory delegateProxyFactory;

  @Transient @Inject protected FeatureFlagService featureFlagService;

  @Transient @Inject private HostService hostService;

  @Transient @Inject protected StateExecutionService stateExecutionService;

  @Transient @Inject @SchemaIgnore protected ServiceResourceService serviceResourceService;

  @Transient @Inject @SchemaIgnore protected K8sStateHelper k8sStateHelper;

  @Inject private transient ExpressionEvaluator evaluator;

  @Inject private AccountService accountService;

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

  public abstract AnalysisComparisonStrategy getComparisonStrategy();

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
              .envName(((ExecutionContextImpl) executionContext).getEnv().getName())
              .workflowName(executionContext.getWorkflowExecutionName())
              .appName(((ExecutionContextImpl) executionContext).getApp().getName())
              .artifactName(artifactForService == null ? null : artifactForService.getDisplayName())
              .serviceName(getPhaseServiceName(executionContext))
              .workflowStartTs(workflowExecution.getStartTs())
              .stateType(StateType.valueOf(getStateType()))
              .stateStartTs(((ExecutionContextImpl) executionContext).getStateExecutionInstance().getStartTs())
              .phaseName(getPhaseName(executionContext))
              .phaseId(getPhaseId(executionContext))
              .executionStatus(ExecutionStatus.RUNNING)
              .envId(((ExecutionContextImpl) executionContext).getEnv().getUuid());

      if (workflowExecution.getPipelineExecutionId() != null) {
        WorkflowExecution pipelineExecutionDetails = workflowExecutionService.getExecutionDetails(
            executionContext.getAppId(), workflowExecution.getPipelineExecutionId(), false, emptySet());
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
    Map<String, String> controlNodes = new HashMap<>();
    context.getControlNodes().forEach((host, groupName) -> controlNodes.put(replaceDotWithUnicode(host), groupName));
    context.setControlNodes(controlNodes);

    Map<String, String> testNodes = new HashMap<>();
    context.getTestNodes().forEach((host, groupName) -> testNodes.put(replaceDotWithUnicode(host), groupName));
    context.setTestNodes(testNodes);

    context.setDelegateTaskId(delegateTaskId);
    return wingsPersistence.save(context);
  }

  protected boolean isTriggerBasedDeployment(ExecutionContext context) {
    WorkflowExecution workflowExecution =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    if (workflowExecution.getTriggeredBy() != null
        && workflowExecution.getTriggeredBy().getName().contains("Deployment Trigger")) {
      return true;
    }
    return false;
  }
  protected Map<String, String> getLastExecutionNodes(ExecutionContext context) {
    String serviceId = getPhaseServiceId(context);
    Service service = serviceResourceService.get(context.getAppId(), serviceId, false);
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    String infraMappingId = infrastructureMapping.getUuid();

    if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      return getPcfHostNames(context, true);
    }

    DeploymentType deploymentType = serviceResourceService.getDeploymentType(infrastructureMapping, null, serviceId);
    Map<String, String> phaseHosts = getHostsDeployedSoFar(context, serviceId, deploymentType);
    getLogger().info("Deployed hosts so far: {}", phaseHosts);

    if (service.isK8sV2()) {
      final Map<String, String> hosts = new HashMap<>();
      K8sElement k8sElement = k8sStateHelper.getK8sElement(context);
      if (k8sElement != null) {
        ContainerInfrastructureMapping containerInfrastructureMapping =
            (ContainerInfrastructureMapping) infrastructureMapping;
        List<K8sPod> currentPods = k8sStateHelper.getPodList(
            containerInfrastructureMapping, containerInfrastructureMapping.getNamespace(), k8sElement.getReleaseName());
        if (currentPods != null) {
          currentPods.forEach(pod -> {
            if (isEmpty(hostnameTemplate)) {
              hosts.put(pod.getName(), DEFAULT_GROUP_NAME);
            } else {
              hosts.put(context.renderExpression(hostnameTemplate,
                            StateExecutionContext.builder()
                                .contextElements(Lists.newArrayList(
                                    aHostElement().withHostName(pod.getName()).withIp(pod.getPodIP()).build()))
                                .build()),
                  DEFAULT_GROUP_NAME);
            }
          });
          phaseHosts.keySet().forEach(host -> hosts.remove(host));
        }
      }
      return hosts;
    } else if (containerInstanceHandler.isContainerDeployment(infrastructureMapping)) {
      Set<ContainerMetadata> containerMetadataSet =
          containerInstanceHandler.getContainerServiceNames(context, serviceId, infraMappingId);

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
                              .withHostName(serviceHost)
                              .withHost(
                                  aHostElement().withHostName(serviceHost).withIp(podNameToIp.get(serviceHost)).build())
                              .build()))
                      .build()),
              DEFAULT_GROUP_NAME);
        }
      });
      phaseHosts.keySet().forEach(host -> hosts.remove(host));
      return hosts;
    }
    int offset = 0;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
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
        phaseHosts.keySet().forEach(host -> hosts.remove(host));
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
        settingsService.get(containerInfrastructureMapping.getComputeProviderSettingId());
    Preconditions.checkNotNull(settingAttribute, "Could not find config for " + containerInfrastructureMapping);
    Preconditions.checkNotNull(settingAttribute.getValue(), "Cloud provider setting not found for " + settingAttribute);
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());
    String accountId = this.appService.get(context.getAppId()).getAccountId();
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(accountId)
                                          .appId(context.getAppId())
                                          .envId(containerInfrastructureMapping.getEnvId())
                                          .infrastructureMappingId(containerInfrastructureMapping.getUuid())
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

    for (software.wings.cloudprovider.ContainerInfo containerInfo : containerInfos) {
      String evaluatedHost = isEmpty(getHostnameTemplate())
          ? containerInfo.getContainerId()
          : context.renderExpression(getHostnameTemplate(),
                StateExecutionContext.builder()
                    .contextElements(
                        Lists.newArrayList(anInstanceElement()
                                               .withHostName(containerInfo.getHostName())
                                               .withPodName(containerInfo.getPodName())
                                               .withWorkloadName(containerInfo.getWorkloadName())
                                               .withHost(aHostElement()
                                                             .withHostName(containerInfo.getHostName())
                                                             .withIp(containerInfo.getIp())
                                                             .withEc2Instance(containerInfo.getEc2Instance())
                                                             .build())
                                               .withEcsContainerDetails(containerInfo.getEcsContainerDetails())
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
              if (isEmpty(getHostnameTemplate())) {
                hosts.put(instanceStatusSummary.getInstanceElement().getHostName(),
                    getGroupName(instanceStatusSummary.getInstanceElement(), deploymentType));
              } else {
                hosts.put(context.renderExpression(getHostnameTemplate(),
                              StateExecutionContext.builder()
                                  .contextElements(Lists.newArrayList(instanceStatusSummary.getInstanceElement()))
                                  .build()),
                    getGroupName(instanceStatusSummary.getInstanceElement(), deploymentType));
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
    return hosts;
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
          if (isNotEmpty(pcfHostName)) {
            if (isEmpty(hostnameTemplate)) {
              rv.put(pcfHostName, DEFAULT_GROUP_NAME);
            } else {
              rv.put(context.renderExpression(hostnameTemplate,
                         StateExecutionContext.builder()
                             .contextElements(Lists.newArrayList(aHostElement().withHostName(pcfHostName).build()))
                             .build()),
                  DEFAULT_GROUP_NAME);
            }
          }
        });
      }
    });
    return rv;
  }

  protected boolean isAwsLambdaState(ExecutionContext context) {
    return getStateType().equals(StateType.CLOUD_WATCH.name())
        && getDeploymentType(context).equals(DeploymentType.AWS_LAMBDA);
  }

  protected boolean isAwsECSState(ExecutionContext context) {
    return getStateType().equals(StateType.CLOUD_WATCH.name()) && getDeploymentType(context).equals(DeploymentType.ECS);
  }

  protected String getPcfHostName(PcfInstanceElement pcfInstanceElement, boolean includePrevious) {
    if (includePrevious || pcfInstanceElement.isUpsize()) {
      return pcfInstanceElement.getApplicationId();
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
    PhaseElement phaseElement = getPhaseElement(context);
    return phaseElement.getInfraMappingId();
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

  public static String generateAuthToken(final String secret) throws UnsupportedEncodingException {
    if (secret == null) {
      throw new InvalidRequestException("No secret present for external service");
    }

    Algorithm algorithm = Algorithm.HMAC256(secret);
    return JWT.create()
        .withIssuer("Harness Inc")
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)))
        .sign(algorithm);
  }

  protected boolean isDemoPath(String accountId) {
    return featureFlagService.isEnabled(FeatureName.CV_DEMO, accountId);
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
    Preconditions.checkNotNull(hostElement, "host element null for " + instanceElement);
    Host host =
        hostService.get(context.getAppId(), ((ExecutionContextImpl) context).getEnv().getUuid(), hostElement.getUuid());
    if (host == null) {
      return;
    }
    instanceElement.setHost(aHostElement()
                                .withHostName(host.getHostName())
                                .withEc2Instance(host.getEc2Instance())
                                .withPublicDns(host.getPublicDns())
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
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PHASE_PARAM);
    String infraMappingId = phaseElement.getInfraMappingId();

    return infraMappingService.get(context.getAppId(), infraMappingId);
  }

  private String getGroupName(InstanceElement instanceElement, DeploymentType deploymentType) {
    if (deploymentType == null || !deploymentType.equals(DeploymentType.HELM)
        || isEmpty(instanceElement.getWorkloadName())) {
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
}

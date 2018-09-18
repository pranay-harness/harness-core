package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.beans.FeatureName.CV_SUCCEED_FOR_ANOMALY;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.InvalidRequestException;
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
import software.wings.app.MainConfiguration;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.common.Constants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsDeque;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData;
import software.wings.service.impl.analysis.ContinuousVerificationExecutionMetaData.ContinuousVerificationExecutionMetaDataBuilder;
import software.wings.service.impl.analysis.ContinuousVerificationService;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.StateExecutionService.CurrentPhase;
import software.wings.service.intfc.WorkflowExecutionBaselineService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

  @Transient @Inject private FeatureFlagService featureFlagService;

  @Transient @Inject private HostService hostService;

  @Transient @Inject protected StateExecutionService stateExecutionService;

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

  public AbstractAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  protected void saveMetaDataForDashboard(String accountId, ExecutionContext executionContext) {
    try {
      WorkflowExecution workflowExecution = workflowExecutionService.getWorkflowExecution(
          executionContext.getAppId(), executionContext.getWorkflowExecutionId());

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
              .artifactName(((ExecutionContextImpl) executionContext)
                                .getArtifactForService(getPhaseServiceId(executionContext))
                                .getDisplayName())
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
        cvExecutionMetaDataBuilder.pipelineName(pipelineExecutionDetails.getName())
            .pipelineStartTs(pipelineExecutionDetails.getStartTs())
            .pipelineExecutionId(workflowExecution.getPipelineExecutionId())
            .pipelineId(workflowExecution.getExecutionArgs().getPipelineId());
      }

      continuousVerificationService.saveCVExecutionMetaData(cvExecutionMetaDataBuilder.build());
    } catch (Exception ex) {
      getLogger().error("[learning-engine] Unable to save ml analysis metadata", ex);
    }
  }

  protected Map<String, String> getLastExecutionNodes(ExecutionContext context) {
    String serviceId = getPhaseServiceId(context);
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    String infraMappingId = infrastructureMapping.getUuid();
    DeploymentType deploymentType = DeploymentType.valueOf(infrastructureMapping.getDeploymentType());

    if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      return getPcfHostNames(context, true);
    }

    Map<String, String> phaseHosts = getHostsDeployedSoFar(context, serviceId, deploymentType);
    getLogger().info("Deployed hosts so far: {}", phaseHosts);

    if (containerInstanceHandler.isContainerDeployment(infrastructureMapping)) {
      Set<String> containerServiceNames =
          containerInstanceHandler.getContainerServiceNames(context, serviceId, infraMappingId);

      if (isEmpty(containerServiceNames)) {
        getLogger().info("state {} has no containers deployed for service {} infra {}. Returning empty",
            context.getStateExecutionInstanceId(), serviceId, infraMappingId);
        return Collections.emptyMap();
      }

      if (infrastructureMapping instanceof EcsInfrastructureMapping) {
        Map<String, String> hosts =
            getEcsLastExecutionNodes(context, (EcsInfrastructureMapping) infrastructureMapping, containerServiceNames);
        phaseHosts.keySet().forEach(host -> hosts.remove(host));
        return hosts;
      }

      List<ContainerInfo> containerInfoForService = containerInstanceHandler.getContainerInfoForService(
          containerServiceNames, context, infraMappingId, serviceId);
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
                                     .collect(Collectors.toSet());
      final Map<String, String> hosts = new HashMap<>();
      serviceHosts.forEach(serviceHost -> {
        if (isEmpty(hostnameTemplate)) {
          hosts.put(serviceHost, DEFAULT_GROUP_NAME);
        } else {
          hosts.put(context.renderExpression(hostnameTemplate,
                        Lists.newArrayList(
                            aHostElement().withHostName(serviceHost).withIp(podNameToIp.get(serviceHost)).build())),
              DEFAULT_GROUP_NAME);
        }
      });
      phaseHosts.keySet().forEach(host -> hosts.remove(host));
      return hosts;
    }
    int offSet = 0;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    final PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .addFilter("appId", Operator.EQ, context.getAppId())
            .addFilter("workflowId", Operator.EQ, getWorkflowId(context))
            .addFilter("_id", Operator.NOT_EQ, context.getWorkflowExecutionId())
            .addFilter("envId", Operator.EQ, envId)
            .addFilter("status", Operator.EQ, SUCCESS)
            .addOrder("createdAt", OrderType.DESC)
            .withOffset(String.valueOf(offSet))
            .withLimit(String.valueOf(PageRequest.DEFAULT_PAGE_SIZE))
            .build();

    PageResponse<WorkflowExecution> workflowExecutions;
    do {
      workflowExecutions = workflowExecutionService.listExecutions(pageRequest, false);

      if (workflowExecutions == null) {
        getLogger().info("Did not find a successful workflow with service {}. It will be a baseline run", serviceId);
        return emptyMap();
      }

      for (WorkflowExecution workflowExecution : workflowExecutions) {
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
              hosts.put(context.renderExpression(
                            getHostnameTemplate(), Lists.newArrayList(instanceStatusSummary.getInstanceElement())),
                  DEFAULT_GROUP_NAME);
            }
          }
          getLogger().info("hosts deployed with last workflow execution: {}", hosts);
          phaseHosts.keySet().forEach(host -> hosts.remove(host));
          return hosts;
        }
      }

      offSet = offSet + PageRequest.DEFAULT_PAGE_SIZE;
      pageRequest.setOffset(String.valueOf(offSet));
    } while (workflowExecutions.size() >= PageRequest.DEFAULT_PAGE_SIZE);

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
    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(accountId)
                                          .withAppId(context.getAppId())
                                          .withEnvId(containerInfrastructureMapping.getEnvId())
                                          .withInfrastructureMappingId(containerInfrastructureMapping.getUuid())
                                          .build();
    syncTaskContext.setTimeout(Constants.DEFAULT_SYNC_CALL_TIMEOUT * 2);
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
    for (software.wings.cloudprovider.ContainerInfo containerInfo : containerInfos) {
      map.put(containerInfo.getContainerId(), DEFAULT_GROUP_NAME);
    }
    return map;
  }

  private Map<String, String> getHostsDeployedSoFar(
      ExecutionContext context, String serviceId, DeploymentType deploymentType) {
    Map<String, String> hosts = new HashMap<>();
    StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();
    Preconditions.checkNotNull(stateExecutionInstance);

    PhaseElement contextPhaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
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
                hosts.put(context.renderExpression(
                              getHostnameTemplate(), Lists.newArrayList(instanceStatusSummary.getInstanceElement())),
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
    DeploymentType deploymentType = DeploymentType.valueOf(infrastructureMapping.getDeploymentType());
    if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      return getPcfHostNames(context, false);
    }

    Map<String, String> rv = new HashMap<>();
    if (includePreviousPhaseNodes) {
      getLogger().info("returning all phases nodes for state {}", context.getStateExecutionInstanceId());
      rv.putAll(getHostsDeployedSoFar(context, getPhaseServiceId(context), getDeploymentType(context)));
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    if (isEmpty(workflowStandardParams.getInstances())) {
      getLogger().warn(
          "No test nodes found for state: {}, id: {} ", getStateType(), context.getStateExecutionInstanceId());
      return rv;
    }
    for (InstanceElement instanceElement : workflowStandardParams.getInstances()) {
      if (isEmpty(getHostnameTemplate())) {
        rv.put(instanceElement.getHostName(), getGroupName(instanceElement, deploymentType));
      } else {
        rv.put(context.renderExpression(getHostnameTemplate(), Lists.newArrayList(instanceElement)),
            getGroupName(instanceElement, deploymentType));
      }
    }
    return rv;
  }

  private Map<String, String> getPcfHostNames(ExecutionContext context, boolean includePrevious) {
    StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();
    WingsDeque<ContextElement> contextElements = stateExecutionInstance.getContextElements();
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
            rv.put(pcfHostName, DEFAULT_GROUP_NAME);
          }
        });
      }
    });
    return rv;
  }

  protected String getPcfHostName(PcfInstanceElement pcfInstanceElement, boolean includePrevious) {
    if (includePrevious || pcfInstanceElement.isUpsize()) {
      return pcfInstanceElement.getApplicationId();
    }

    return null;
  }

  protected String getPhaseServiceId(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return phaseElement.getServiceElement().getUuid();
  }

  protected DeploymentType getDeploymentType(ExecutionContext context) {
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    return DeploymentType.valueOf(infrastructureMapping.getDeploymentType());
  }

  protected String getPhaseServiceName(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return phaseElement.getServiceElement().getName();
  }

  protected String getPhaseName(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return phaseElement.getPhaseName();
  }

  protected String getWorkflowId(ExecutionContext context) {
    final WorkflowExecution executionDetails =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    return executionDetails.getWorkflowId();
  }

  protected String getPhaseId(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return phaseElement.getUuid();
  }

  @SchemaIgnore public abstract Logger getLogger();

  public abstract String getAnalysisServerConfigId();

  public abstract void setAnalysisServerConfigId(String analysisServerConfigId);

  protected String generateAuthToken() throws UnsupportedEncodingException {
    return generateAuthToken(configuration.getPortal().getJwtExternalServiceSecret());
  }

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
    return Constants.DEFAULT_VERIFICATION_STATE_TIMEOUT_MILLIS;
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
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
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
}

package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import software.wings.api.CanaryWorkflowStandardParams;
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
import software.wings.exception.InvalidRequestException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisContext;
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
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
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

  protected String hostnameField;

  protected String hostnameTemplate;

  protected AnalysisContext analysisContext;

  @Attributes(title = "Analysis Time duration (in minutes)")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
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
            executionContext.getAppId(), workflowExecution.getPipelineExecutionId(), emptySet());
        cvExecutionMetaDataBuilder.pipelineName(pipelineExecutionDetails.getName())
            .pipelineStartTs(pipelineExecutionDetails.getStartTs())
            .pipelineId(pipelineExecutionDetails.getPipelineExecutionId());
      }

      continuousVerificationService.saveCVExecutionMetaData(cvExecutionMetaDataBuilder.build());
    } catch (Exception ex) {
      getLogger().error("[learning-engine] Unable to save ml analysis metadata", ex);
    }
  }

  protected Set<String> getLastExecutionNodes(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    String infraMappingId = phaseElement.getInfraMappingId();

    InfrastructureMapping infrastructureMapping = infraMappingService.get(context.getAppId(), infraMappingId);

    if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      return getPcfHostNames(context, true);
    }

    Set<String> phaseHosts = getHostsDeployedSoFar(context, serviceId);
    getLogger().info("Deployed hosts so far: {}", phaseHosts);

    if (containerInstanceHandler.isContainerDeployment(infrastructureMapping)) {
      Set<String> containerServiceNames =
          containerInstanceHandler.getContainerServiceNames(context, serviceId, infraMappingId);

      if (infrastructureMapping instanceof EcsInfrastructureMapping) {
        Set<String> hosts =
            getEcsLastExecutionNodes(context, (EcsInfrastructureMapping) infrastructureMapping, containerServiceNames);
        hosts.removeAll(phaseHosts);
        return hosts;
      }

      List<ContainerInfo> containerInfoForService = containerInstanceHandler.getContainerInfoForService(
          containerServiceNames, context, infraMappingId, serviceId);
      Set<String> serviceHosts = containerInfoForService.stream()
                                     .map(containerInfo -> {
                                       if (containerInfo instanceof KubernetesContainerInfo) {
                                         return ((KubernetesContainerInfo) containerInfo).getPodName();
                                       }

                                       if (containerInfo instanceof EcsContainerInfo) {
                                         return ((EcsContainerInfo) containerInfo).getServiceName();
                                       }

                                       throw new IllegalStateException("Invalid type " + containerInfo);
                                     })
                                     .collect(Collectors.toSet());
      final Set<String> hosts = new HashSet<>();
      serviceHosts.forEach(serviceHost -> {
        if (isEmpty(hostnameTemplate)) {
          hosts.add(serviceHost);
        } else {
          hosts.add(context.renderExpression(
              hostnameTemplate, Lists.newArrayList(aHostElement().withHostName(serviceHost).build())));
        }
      });
      hosts.removeAll(phaseHosts);
      return hosts;
    }
    int offSet = 0;
    final PageRequest<WorkflowExecution> pageRequest =
        aPageRequest()
            .addFilter("appId", Operator.EQ, context.getAppId())
            .addFilter("workflowId", Operator.EQ, getWorkflowId(context))
            .addFilter("_id", Operator.NOT_EQ, context.getWorkflowExecutionId())
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
        return emptySet();
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
            return emptySet();
          }
          Set<String> hosts = new HashSet<>();
          for (InstanceStatusSummary instanceStatusSummary : executionSummary.getInstanceStatusSummaries()) {
            if (isEmpty(getHostnameTemplate())) {
              hosts.add(instanceStatusSummary.getInstanceElement().getHostName());
            } else {
              fillHostDetail(instanceStatusSummary.getInstanceElement(), context);
              hosts.add(context.renderExpression(
                  getHostnameTemplate(), Lists.newArrayList(instanceStatusSummary.getInstanceElement())));
            }
          }
          getLogger().info("hosts deployed with last workflow execution: {}", hosts);
          hosts.removeAll(phaseHosts);
          return hosts;
        }
      }

      offSet = offSet + PageRequest.DEFAULT_PAGE_SIZE;
      pageRequest.setOffset(String.valueOf(offSet));
    } while (workflowExecutions.size() >= PageRequest.DEFAULT_PAGE_SIZE);

    getLogger().info("Did not find a successful workflow with service {}. It will be a baseline run", serviceId);
    return emptySet();
  }

  private Set<String> getEcsLastExecutionNodes(ExecutionContext context,
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

    return containerInfos.stream().map(containerInfo -> containerInfo.getContainerId()).collect(Collectors.toSet());
  }

  private Set<String> getHostsDeployedSoFar(ExecutionContext context, String serviceId) {
    Set<String> hosts = new HashSet<>();
    StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();
    Preconditions.checkNotNull(stateExecutionInstance);
    Map<String, StateExecutionData> stateExecutionMap = stateExecutionInstance.getStateExecutionMap();

    if (stateExecutionMap == null) {
      return hosts;
    }
    stateExecutionMap.values()
        .stream()
        .filter(stateExecutionData -> stateExecutionData.getStateType().equals(StateType.PHASE.name()))
        .forEach(stateExecutionData -> {
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
                    hosts.add(instanceStatusSummary.getInstanceElement().getHostName());
                  } else {
                    hosts.add(context.renderExpression(
                        getHostnameTemplate(), Lists.newArrayList(instanceStatusSummary.getInstanceElement())));
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

  protected Set<String> getCanaryNewHostNames(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infraMappingId = phaseElement.getInfraMappingId();

    InfrastructureMapping infrastructureMapping = infraMappingService.get(context.getAppId(), infraMappingId);
    if (infrastructureMapping instanceof PcfInfrastructureMapping) {
      return getPcfHostNames(context, false);
    }

    CanaryWorkflowStandardParams canaryWorkflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Set<String> rv = new HashSet<>();
    if (isEmpty(canaryWorkflowStandardParams.getInstances())) {
      getLogger().warn(
          "No test nodes found for state: {}, id: {} ", getStateType(), context.getStateExecutionInstanceId());
      return rv;
    }
    for (InstanceElement instanceElement : canaryWorkflowStandardParams.getInstances()) {
      if (isEmpty(getHostnameTemplate())) {
        rv.add(instanceElement.getHostName());
      } else {
        rv.add(context.renderExpression(getHostnameTemplate(), Lists.newArrayList(instanceElement)));
      }
    }
    return rv;
  }

  private Set<String> getPcfHostNames(ExecutionContext context, boolean includePrevious) {
    StateExecutionInstance stateExecutionInstance = ((ExecutionContextImpl) context).getStateExecutionInstance();
    WingsDeque<ContextElement> contextElements = stateExecutionInstance.getContextElements();
    Set<String> rv = new HashSet<>();
    if (isEmpty(contextElements)) {
      return rv;
    }

    contextElements.forEach(contextElement -> {
      if (contextElement instanceof InstanceElementListParam) {
        InstanceElementListParam instances = (InstanceElementListParam) contextElement;
        instances.getPcfInstanceElements().forEach(pcfInstanceElement -> {
          String pcfHostName = getPcfHostName(pcfInstanceElement, includePrevious);
          if (isNotEmpty(pcfHostName)) {
            rv.add(pcfHostName);
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

  protected abstract String triggerAnalysisDataCollection(
      ExecutionContext context, String correlationId, Set<String> hosts);

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
}

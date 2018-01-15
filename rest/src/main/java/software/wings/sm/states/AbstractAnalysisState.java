package software.wings.sm.states;

import static org.apache.commons.lang.StringUtils.isBlank;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;

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
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.instance.ContainerInstanceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.stencils.DefaultValue;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractAnalysisState extends State {
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

  @Transient @Inject @SchemaIgnore protected ContainerInstanceHelper containerInstanceHelper;

  @Transient @Inject @SchemaIgnore protected InfrastructureMappingService infraMappingService;

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

  protected Set<String> getLastExecutionNodes(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    String infraMappingId = phaseElement.getInfraMappingId();

    if (containerInstanceHelper.isContainerDeployment(infraMappingService.get(context.getAppId(), infraMappingId))) {
      Set<String> containerServiceNames =
          containerInstanceHelper.getContainerServiceNames(context, serviceId, infraMappingId);
      List<ContainerInfo> containerInfoForService =
          containerInstanceHelper.getContainerInfoForService(containerServiceNames, context, infraMappingId, serviceId);
      return containerInfoForService.stream()
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
    }
    final PageRequest<WorkflowExecution> pageRequest =
        PageRequest.Builder.aPageRequest()
            .addFilter("appId", Operator.EQ, context.getAppId())
            .addFilter("workflowId", Operator.EQ, getWorkflowId(context))
            .addFilter("_id", Operator.NOT_EQ, context.getWorkflowExecutionId())
            .addFilter("status", Operator.EQ, ExecutionStatus.SUCCESS)
            .addOrder("createdAt", OrderType.DESC)
            .withLimit("1")
            .build();

    final PageResponse<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false);
    if (workflowExecutions.isEmpty()) {
      getLogger().warn("Could not get a successful workflow to find control nodes");
      return new HashSet<>();
    }

    Preconditions.checkState(workflowExecutions.size() == 1, "Multiple workflows found for give query");
    final WorkflowExecution workflowExecution = workflowExecutions.get(0);

    ElementExecutionSummary executionSummary = null;
    for (ElementExecutionSummary summary : workflowExecution.getServiceExecutionSummaries()) {
      if (summary.getContextElement().getUuid().equals(serviceId)) {
        executionSummary = summary;
        break;
      }
    }

    Preconditions.checkNotNull(executionSummary, "could not find the execution summary for current execution");

    Set<String> hosts = new HashSet<>();
    for (InstanceStatusSummary instanceStatusSummary : executionSummary.getInstanceStatusSummaries()) {
      hosts.add(instanceStatusSummary.getInstanceElement().getHostName());
    }

    hosts = hosts.stream()
                .flatMap(hostname
                    -> hostname.contains(".") ? Lists.newArrayList(hostname.split("\\.")[0], hostname).stream()
                                              : Stream.of(hostname))
                .collect(Collectors.toSet());

    return hosts;
  }

  protected Set<String> getCanaryNewHostNames(ExecutionContext context) {
    CanaryWorkflowStandardParams canaryWorkflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Set<String> rv = new HashSet<>();
    for (InstanceElement instanceElement : canaryWorkflowStandardParams.getInstances()) {
      rv.add(instanceElement.getHostName());
    }

    rv = rv.stream()
             .flatMap(hostname
                 -> hostname.contains(".") ? Lists.newArrayList(hostname.split("\\.")[0], hostname).stream()
                                           : Stream.of(hostname))
             .collect(Collectors.toSet());

    return rv;
  }

  protected String getPhaseServiceId(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return phaseElement.getServiceElement().getUuid();
  }

  protected String getWorkflowId(ExecutionContext context) {
    final WorkflowExecution executionDetails =
        workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
    return executionDetails.getWorkflowId();
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
      throw new WingsException(INVALID_REQUEST).addParam("message", "No secret present for external service");
    }

    Algorithm algorithm = Algorithm.HMAC256(secret);
    return JWT.create()
        .withIssuer("Harness Inc")
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)))
        .sign(algorithm);
  }
}

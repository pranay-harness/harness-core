package software.wings.sm.states;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import software.wings.AnalysisComparisonStrategy;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.app.MainConfiguration;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.analysis.AnalysisService;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 7/6/17.
 */
public abstract class AbstractAnalysisState extends State {
  public static final int PYTHON_JOB_RETRIES = 3;

  protected String timeDuration;
  protected String comparisonStrategy;

  @Transient @Inject protected WorkflowExecutionService workflowExecutionService;

  @Transient @Inject protected WaitNotifyEngine waitNotifyEngine;

  @Transient @Inject protected SettingsService settingsService;

  @Inject @Transient protected WingsPersistence wingsPersistence;

  @Transient @Inject protected AppService appService;

  @Transient @Inject protected DelegateService delegateService;

  @Transient @Inject @SchemaIgnore protected MainConfiguration configuration;

  @Transient @Inject protected AnalysisService analysisService;

  @DefaultValue("15")
  @Attributes(title = "Analyze Time duration (in minutes)", description = "Default 15 minutes")
  public String getTimeDuration() {
    return timeDuration;
  }

  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  @DefaultValue("COMPARE_WITH_PREVIOUS")
  @Attributes(required = true, title = "How do you want to compare for analyis",
      description = "Compare with previous run or current run")
  public AnalysisComparisonStrategy
  getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  public void setComparisonStrategy(String comparisonStrategy) {
    this.comparisonStrategy = comparisonStrategy;
  }

  public AbstractAnalysisState(String name, String stateType) {
    super(name, stateType);
  }

  protected Set<String> getLastExecutionNodes(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

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
      getLogger().error("Could not get a successful workflow to find control nodes");
      return null;
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

    return hosts;
  }

  protected Set<String> getCanaryNewHostNames(ExecutionContext context) {
    CanaryWorkflowStandardParams canaryWorkflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    final Set<String> rv = new HashSet<>();
    for (InstanceElement instanceElement : canaryWorkflowStandardParams.getInstances()) {
      rv.add(instanceElement.getHostName());
    }

    return rv;
  }

  protected String getPhaseServiceId(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    return phaseElement.getServiceElement().getUuid();
  }

  protected String getWorkflowId(ExecutionContext context) {
    final WorkflowExecution executionDetails =
        workflowExecutionService.getExecutionDetails(context.getAppId(), context.getWorkflowExecutionId());
    return executionDetails.getWorkflowId();
  }

  @SchemaIgnore abstract public Logger getLogger();

  abstract public String getAnalysisServerConfigId();

  abstract public void setAnalysisServerConfigId(String analysisServerConfigId);

  protected abstract void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts);

  protected String generateAuthToken() throws UnsupportedEncodingException {
    return generateAuthToken(configuration.getPortal().getJwtExternalServiceSecret());
  }

  public static String generateAuthToken(final String secret) throws UnsupportedEncodingException {
    if (secret == null) {
      throw new WingsException(INVALID_REQUEST, "message", "No secret present for external service");
    }

    Algorithm algorithm = Algorithm.HMAC256(secret);
    return JWT.create()
        .withIssuer("Harness Inc")
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)))
        .sign(algorithm);
  }
}

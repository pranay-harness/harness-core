/**
 *
 */

package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.MoreObjects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.common.Constants;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.PipelineSummary;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class WorkflowExecution.
 *
 * @author Rishi
 */
@Entity(value = "workflowExecutions", noClassnameStored = true)
@Indexes(@Index(options = @IndexOptions(name = "search"), fields = { @Field("workflowId")
                                                                     , @Field("status") }))
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class WorkflowExecution extends Base {
  public static final String ARGS_PIPELINE_PHASE_ELEMENT_ID_KEY = "executionArgs.pipelinePhaseElementId";
  public static final String PIPELINE_EXECUTION_ID_KEY = "pipelineExecutionId";
  public static final String START_TS_KEY = "startTs";
  public static final String STATUS_KEY = "status";
  public static final String WORKFLOW_ID_KEY = "workflowId";
  public static final String WORKFLOW_TYPE_ID_KEY = "workflowType";

  // TODO: Determine the right expiry duration for workflow exceptions
  public static final Duration EXPIRY = Duration.ofDays(7);

  @Indexed private String workflowId;

  private String stateMachineId;
  @Indexed private String envId;
  @Indexed private List<String> envIds;
  @Indexed private List<String> serviceIds;
  private String appName;
  private String envName;
  private EnvironmentType envType;
  @Indexed private WorkflowType workflowType;
  @Indexed private ExecutionStatus status = ExecutionStatus.NEW;
  @Transient private Graph graph;

  @Transient private GraphNode executionNode; // used for workflow details.
  private PipelineExecution pipelineExecution; // used for pipeline details.

  @Indexed private String pipelineExecutionId;
  private ErrorStrategy errorStrategy;

  private String name;
  private String displayName;
  private String releaseNo;
  private int total;
  private CountsByStatuses breakdown;

  private ExecutionArgs executionArgs;
  private List<ElementExecutionSummary> serviceExecutionSummaries;
  private LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap;

  private Long startTs;
  private Long endTs;

  private EmbeddedUser triggeredBy;

  private PipelineSummary pipelineSummary;

  private List<BuildExecutionSummary> buildExecutionSummaries;

  private OrchestrationWorkflowType orchestrationType;

  private boolean isBaseline;

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    if (isBlank(name)) {
      if (pipelineExecution != null && pipelineExecution.getPipeline() != null
          && isNotBlank(pipelineExecution.getPipeline().getName())) {
        return pipelineExecution.getPipeline().getName();
      }
      return String.valueOf(workflowType);
    }
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  public String getDisplayName() {
    if (displayName == null) {
      return prepareDisplayName();
    }
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getReleaseNo() {
    return releaseNo;
  }

  public void setReleaseNo(String releaseNo) {
    this.releaseNo = releaseNo;
  }

  /**
   * Gets workflow id.
   *
   * @return the workflow id
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets workflow id.
   *
   * @param workflowId the workflow id
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  /**
   * Gets state machine id.
   *
   * @return the state machine id
   */
  public String getStateMachineId() {
    return stateMachineId;
  }

  /**
   * Sets state machine id.
   *
   * @param stateMachineId the state machine id
   */
  public void setStateMachineId(String stateMachineId) {
    this.stateMachineId = stateMachineId;
  }

  public String getPipelineExecutionId() {
    return pipelineExecutionId;
  }

  public void setPipelineExecutionId(String pipelineExecutionId) {
    this.pipelineExecutionId = pipelineExecutionId;
  }

  /**
   * Gets workflow type.
   *
   * @return the workflow type
   */
  public WorkflowType getWorkflowType() {
    return workflowType;
  }

  /**
   * Sets workflow type.
   *
   * @param workflowType the workflow type
   */
  public void setWorkflowType(WorkflowType workflowType) {
    this.workflowType = workflowType;
  }

  /**
   * Gets graph.
   *
   * @return the graph
   */
  public Graph getGraph() {
    return graph;
  }

  /**
   * Sets graph.
   *
   * @param graph the graph
   */
  public void setGraph(Graph graph) {
    this.graph = graph;
  }

  /**
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Getter for property 'total'.
   *
   * @return Value for property 'total'.
   */
  public int getTotal() {
    return total;
  }

  /**
   * Setter for property 'total'.
   *
   * @param total Value to set for property 'total'.
   */
  public void setTotal(int total) {
    this.total = total;
  }

  public PipelineExecution getPipelineExecution() {
    return pipelineExecution;
  }

  public void setPipelineExecution(PipelineExecution pipelineExecution) {
    this.pipelineExecution = pipelineExecution;
  }

  /**
   * Gets breakdown.
   *
   * @return the breakdown
   */
  public CountsByStatuses getBreakdown() {
    return breakdown;
  }

  /**
   * Sets breakdown.
   *
   * @param breakdown the breakdown
   */
  public void setBreakdown(CountsByStatuses breakdown) {
    this.breakdown = breakdown;
  }

  /**
   * Gets execution args.
   *
   * @return the execution args
   */
  public ExecutionArgs getExecutionArgs() {
    return executionArgs;
  }

  /**
   * Sets execution args.
   *
   * @param executionArgs the execution args
   */
  public void setExecutionArgs(ExecutionArgs executionArgs) {
    this.executionArgs = executionArgs;
  }

  /**
   * Gets service execution summaries.
   *
   * @return the service execution summaries
   */
  public List<ElementExecutionSummary> getServiceExecutionSummaries() {
    return serviceExecutionSummaries;
  }

  /**
   * Sets service execution summaries.
   *
   * @param serviceExecutionSummaries the service execution summaries
   */
  public void setServiceExecutionSummaries(List<ElementExecutionSummary> serviceExecutionSummaries) {
    this.serviceExecutionSummaries = serviceExecutionSummaries;
  }

  /**
   * Gets status instance breakdown map.
   *
   * @return the status instance breakdown map
   */
  public LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> getStatusInstanceBreakdownMap() {
    return statusInstanceBreakdownMap;
  }

  /**
   * Sets status instance breakdown map.
   *
   * @param statusInstanceBreakdownMap the status instance breakdown map
   */
  public void setStatusInstanceBreakdownMap(
      LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap) {
    this.statusInstanceBreakdownMap = statusInstanceBreakdownMap;
  }

  /**
   * Gets execution node.
   *
   * @return the execution node
   */
  public GraphNode getExecutionNode() {
    return executionNode;
  }

  /**
   * Sets execution node.
   *
   * @param executionNode the execution node
   */
  public void setExecutionNode(GraphNode executionNode) {
    this.executionNode = executionNode;
  }

  /**
   * Is running status boolean.
   *
   * @return the boolean
   */
  public boolean isRunningStatus() {
    return ExecutionStatus.isRunningStatus(status);
  }

  /**
   * Is paused status boolean.
   *
   * @return the boolean
   */
  public boolean isPausedStatus() {
    return status != null && (status == ExecutionStatus.PAUSED || status == ExecutionStatus.WAITING);
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets error strategy.
   *
   * @return the error strategy
   */
  public ErrorStrategy getErrorStrategy() {
    return errorStrategy;
  }

  /**
   * Sets error strategy.
   *
   * @param errorStrategy the error strategy
   */
  public void setErrorStrategy(ErrorStrategy errorStrategy) {
    this.errorStrategy = errorStrategy;
  }

  /**
   * Gets app name.
   *
   * @return the app name
   */
  public String getAppName() {
    return appName;
  }

  /**
   * Sets app name.
   *
   * @param appName the app name
   */
  public void setAppName(String appName) {
    this.appName = appName;
  }

  /**
   * Gets env name.
   *
   * @return the env name
   */
  public String getEnvName() {
    return envName;
  }

  /**
   * Sets env name.
   *
   * @param envName the env name
   */
  public void setEnvName(String envName) {
    this.envName = envName;
  }

  public EnvironmentType getEnvType() {
    return envType;
  }

  public void setEnvType(EnvironmentType envType) {
    this.envType = envType;
  }

  public EmbeddedUser getTriggeredBy() {
    return triggeredBy;
  }

  public void setTriggeredBy(EmbeddedUser triggeredBy) {
    this.triggeredBy = triggeredBy;
  }

  public List<String> getEnvIds() {
    return envIds;
  }

  public void setEnvIds(List<String> envIds) {
    this.envIds = envIds;
  }

  public List<String> getServiceIds() {
    return serviceIds;
  }

  public void setServiceIds(List<String> serviceIds) {
    this.serviceIds = serviceIds;
  }

  public PipelineSummary getPipelineSummary() {
    return pipelineSummary;
  }

  public void setPipelineSummary(PipelineSummary pipelineSummary) {
    this.pipelineSummary = pipelineSummary;
  }

  public List<BuildExecutionSummary> getBuildExecutionSummaries() {
    return buildExecutionSummaries;
  }

  public void setBuildExecutionSummaries(List<BuildExecutionSummary> buildExecutionSummaries) {
    this.buildExecutionSummaries = buildExecutionSummaries;
  }

  public OrchestrationWorkflowType getOrchestrationType() {
    return orchestrationType;
  }

  public void setOrchestrationType(OrchestrationWorkflowType orchestrationType) {
    this.orchestrationType = orchestrationType;
  }

  public boolean isBaseline() {
    return isBaseline;
  }

  public void setBaseline(boolean baseline) {
    isBaseline = baseline;
  }

  public String prepareDisplayName() {
    String dateSuffix = "";
    if (getCreatedAt() != 0) {
      dateSuffix = " - "
          + Instant.ofEpochMilli(getCreatedAt())
                .atZone(ZoneId.of("America/Los_Angeles"))
                .format(DateTimeFormatter.ofPattern(Constants.WORKFLOW_NAME_DATE_FORMAT));
    }
    return name + dateSuffix;
  }

  public void onSave() {
    super.onSave();
    displayName = prepareDisplayName();
  }

  public static final class WorkflowExecutionBuilder {
    private String workflowId;
    private String stateMachineId;
    private String envId;
    private String appName;
    private String envName;
    private EnvironmentType envType;
    private WorkflowType workflowType;
    private ExecutionStatus status = ExecutionStatus.NEW;
    private Graph graph;
    private GraphNode executionNode;
    private ErrorStrategy errorStrategy;
    private String name;
    private int total;
    private CountsByStatuses breakdown;
    private ExecutionArgs executionArgs;
    private List<ElementExecutionSummary> serviceExecutionSummaries;
    private String uuid;
    private LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap;
    private String appId;
    private EmbeddedUser createdBy;
    private Long startTs;
    private Long endTs;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private EmbeddedUser triggeredBy;
    private Map<String, InfraMappingSummary> infraMappingSummary;
    private PipelineSummary pipelineSummary;
    private List<String> serviceIds;
    private List<String> envIds;
    private List<BuildExecutionSummary> buildExecutionSummaries;
    private OrchestrationWorkflowType orchestrationWorkflowType;
    private String pipelineExecutionId;

    private WorkflowExecutionBuilder() {}

    public static WorkflowExecutionBuilder aWorkflowExecution() {
      return new WorkflowExecutionBuilder();
    }

    public WorkflowExecutionBuilder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public WorkflowExecutionBuilder withStateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    public WorkflowExecutionBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public WorkflowExecutionBuilder withAppName(String appName) {
      this.appName = appName;
      return this;
    }

    public WorkflowExecutionBuilder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public WorkflowExecutionBuilder withEnvType(EnvironmentType envType) {
      this.envType = envType;
      return this;
    }

    public WorkflowExecutionBuilder withWorkflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public WorkflowExecutionBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public WorkflowExecutionBuilder withGraph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public WorkflowExecutionBuilder withExecutionNode(GraphNode executionNode) {
      this.executionNode = executionNode;
      return this;
    }

    public WorkflowExecutionBuilder withErrorStrategy(ErrorStrategy errorStrategy) {
      this.errorStrategy = errorStrategy;
      return this;
    }

    public WorkflowExecutionBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public WorkflowExecutionBuilder withTotal(int total) {
      this.total = total;
      return this;
    }

    public WorkflowExecutionBuilder withBreakdown(CountsByStatuses breakdown) {
      this.breakdown = breakdown;
      return this;
    }

    public WorkflowExecutionBuilder withExecutionArgs(ExecutionArgs executionArgs) {
      this.executionArgs = executionArgs;
      return this;
    }

    public WorkflowExecutionBuilder withServiceExecutionSummaries(
        List<ElementExecutionSummary> serviceExecutionSummaries) {
      this.serviceExecutionSummaries = serviceExecutionSummaries;
      return this;
    }

    public WorkflowExecutionBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public WorkflowExecutionBuilder withStatusInstanceBreakdownMap(
        LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap) {
      this.statusInstanceBreakdownMap = statusInstanceBreakdownMap;
      return this;
    }

    public WorkflowExecutionBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public WorkflowExecutionBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public WorkflowExecutionBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public WorkflowExecutionBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public WorkflowExecutionBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public WorkflowExecutionBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public WorkflowExecutionBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }
    public WorkflowExecutionBuilder withTriggeredBy(EmbeddedUser triggeredBy) {
      this.triggeredBy = triggeredBy;
      return this;
    }
    public WorkflowExecutionBuilder withPipelineSummary(PipelineSummary pipelineSummary) {
      this.pipelineSummary = pipelineSummary;
      return this;
    }
    public WorkflowExecutionBuilder withInfraMappingSummary(Map<String, InfraMappingSummary> infraMappingSummary) {
      this.infraMappingSummary = infraMappingSummary;
      return this;
    }

    public WorkflowExecutionBuilder withServiceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    public WorkflowExecutionBuilder withEnvIds(List<String> envIds) {
      this.envIds = envIds;
      return this;
    }

    public WorkflowExecutionBuilder withBuildExecutionSummaries(List<BuildExecutionSummary> buildExecutionSummaries) {
      this.buildExecutionSummaries = buildExecutionSummaries;
      return this;
    }

    public WorkflowExecutionBuilder withOrchestratonWorkflowType(OrchestrationWorkflowType orchestratonWorkflowType) {
      this.orchestrationWorkflowType = orchestratonWorkflowType;
      return this;
    }

    public WorkflowExecutionBuilder withPipelineExecutionId(String pipelineExecutionId) {
      this.pipelineExecutionId = pipelineExecutionId;
      return this;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("workflowId", workflowId)
          .add("stateMachineId", stateMachineId)
          .add("envId", envId)
          .add("appName", appName)
          .add("envName", envName)
          .add("uuid", uuid)
          .add("appId", appId)
          .toString();
    }

    public WorkflowExecutionBuilder but() {
      return aWorkflowExecution()
          .withWorkflowId(workflowId)
          .withStateMachineId(stateMachineId)
          .withEnvId(envId)
          .withAppName(appName)
          .withEnvName(envName)
          .withEnvType(envType)
          .withWorkflowType(workflowType)
          .withStatus(status)
          .withGraph(graph)
          .withExecutionNode(executionNode)
          .withErrorStrategy(errorStrategy)
          .withName(name)
          .withTotal(total)
          .withBreakdown(breakdown)
          .withExecutionArgs(executionArgs)
          .withServiceExecutionSummaries(serviceExecutionSummaries)
          .withUuid(uuid)
          .withStatusInstanceBreakdownMap(statusInstanceBreakdownMap)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withTriggeredBy(triggeredBy)
          .withPipelineSummary(pipelineSummary)
          .withInfraMappingSummary(infraMappingSummary)
          .withServiceIds(serviceIds)
          .withEnvIds(envIds)
          .withBuildExecutionSummaries(buildExecutionSummaries)
          .withOrchestratonWorkflowType(orchestrationWorkflowType)
          .withPipelineExecutionId(pipelineExecutionId);
    }

    public WorkflowExecution build() {
      WorkflowExecution workflowExecution = new WorkflowExecution();
      workflowExecution.setWorkflowId(workflowId);
      workflowExecution.setStateMachineId(stateMachineId);
      workflowExecution.setEnvId(envId);
      workflowExecution.setAppName(appName);
      workflowExecution.setEnvName(envName);
      workflowExecution.setEnvType(envType);
      workflowExecution.setWorkflowType(workflowType);
      workflowExecution.setStatus(status);
      workflowExecution.setGraph(graph);
      workflowExecution.setExecutionNode(executionNode);
      workflowExecution.setErrorStrategy(errorStrategy);
      workflowExecution.setName(name);
      workflowExecution.setTotal(total);
      workflowExecution.setBreakdown(breakdown);
      workflowExecution.setExecutionArgs(executionArgs);
      workflowExecution.setServiceExecutionSummaries(serviceExecutionSummaries);
      workflowExecution.setUuid(uuid);
      workflowExecution.setStatusInstanceBreakdownMap(statusInstanceBreakdownMap);
      workflowExecution.setAppId(appId);
      workflowExecution.setCreatedBy(createdBy);
      workflowExecution.setStartTs(startTs);
      workflowExecution.setEndTs(endTs);
      workflowExecution.setCreatedAt(createdAt);
      workflowExecution.setLastUpdatedBy(lastUpdatedBy);
      workflowExecution.setLastUpdatedAt(lastUpdatedAt);
      workflowExecution.setTriggeredBy(triggeredBy);
      workflowExecution.setPipelineSummary(pipelineSummary);
      workflowExecution.setServiceIds(serviceIds);
      workflowExecution.setEnvIds(envIds);
      workflowExecution.setBuildExecutionSummaries(buildExecutionSummaries);
      workflowExecution.setOrchestrationType(orchestrationWorkflowType);
      workflowExecution.setPipelineExecutionId(pipelineExecutionId);
      return workflowExecution;
    }
  }
}

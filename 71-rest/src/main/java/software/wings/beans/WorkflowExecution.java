/**
 *
 */

package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.artifact.Artifact;
import software.wings.common.Constants;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.PipelineSummary;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * The Class WorkflowExecution.
 */
@Data
@Entity(value = "workflowExecutions", noClassnameStored = true)
@Indexes(@Index(options = @IndexOptions(name = "search"), fields = { @Field("workflowId")
                                                                     , @Field("status") }))
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowExecution implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware {
  public static final String APP_ID_KEY = "appId";
  public static final String ARGS_PIPELINE_PHASE_ELEMENT_ID_KEY = "executionArgs.pipelinePhaseElementId";
  public static final String ARTIFACTS_KEY = "artifacts";
  public static final String DEPLOYMENT_TRIGGERED_ID_KEY = "deploymentTriggerId";
  public static final String END_TS_KEY = "endTs";
  public static final String ENV_ID_KEY = "envId";
  public static final String EXECUTION_ARGS = "executionArgs";
  public static final String INFRA_MAPPING_IDS_KEY = "infraMappingIds";
  public static final String NAME_KEY = "name";
  public static final String PIPELINE_EXECUTION_ID_KEY = "pipelineExecutionId";
  public static final String SERVICE_EXECUTION_SUMMARIES = "serviceExecutionSummaries";
  public static final String START_TS_KEY = "startTs";
  public static final String STATUS_KEY = "status";
  public static final String TRIGGERED_BY = "triggeredBy";
  public static final String UUID_KEY = "uuid";
  public static final String WORKFLOW_ID_KEY = "workflowId";
  public static final String WORKFLOW_TYPE_ID_KEY = "workflowType";

  // TODO: Determine the right expiry duration for workflow exceptions
  public static final Duration EXPIRY = Duration.ofDays(7);

  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
  @Indexed @NotNull @SchemaIgnore protected String appId;
  @SchemaIgnore private EmbeddedUser createdBy;
  @SchemaIgnore @Indexed private long createdAt;

  private String workflowId;

  private String stateMachineId;
  @Indexed private String envId;
  private List<String> envIds;
  @Indexed private List<String> serviceIds;
  private List<String> infraMappingIds;
  private String appName;
  private String envName;
  private EnvironmentType envType;
  private WorkflowType workflowType;
  @Indexed private ExecutionStatus status;
  @Transient private Graph graph;

  @Transient private GraphNode executionNode; // used for workflow details.
  private PipelineExecution pipelineExecution; // used for pipeline details.

  @Indexed private String pipelineExecutionId;
  private ErrorStrategy errorStrategy;

  private String name;
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

  private List<EnvSummary> environments;

  private List<BuildExecutionSummary> buildExecutionSummaries;

  private OrchestrationWorkflowType orchestrationType;

  private boolean isBaseline;

  private String deploymentTriggerId;

  private List<Artifact> artifacts;

  @SchemaIgnore @Indexed private List<String> keywords;

  @SchemaIgnore
  @JsonIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

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

  // TODO: this is silly, we should get rid of it
  public String displayName() {
    String dateSuffix = "";
    if (getCreatedAt() != 0) {
      dateSuffix = " - "
          + Instant.ofEpochMilli(getCreatedAt())
                .atZone(ZoneId.of("America/Los_Angeles"))
                .format(DateTimeFormatter.ofPattern(Constants.WORKFLOW_NAME_DATE_FORMAT));
    }
    return name + dateSuffix;
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
    private EmbeddedUser triggeredBy;
    private Map<String, InfraMappingSummary> infraMappingSummary;
    private PipelineSummary pipelineSummary;
    private List<String> serviceIds;
    private List<String> envIds;
    private List<String> infraMappingIds;
    private List<BuildExecutionSummary> buildExecutionSummaries;
    private OrchestrationWorkflowType orchestrationWorkflowType;
    private String pipelineExecutionId;
    private PipelineExecution pipelineExecution;

    private WorkflowExecutionBuilder() {}

    public static WorkflowExecutionBuilder aWorkflowExecution() {
      return new WorkflowExecutionBuilder();
    }

    public WorkflowExecutionBuilder workflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public WorkflowExecutionBuilder stateMachineId(String stateMachineId) {
      this.stateMachineId = stateMachineId;
      return this;
    }

    public WorkflowExecutionBuilder envId(String envId) {
      this.envId = envId;
      return this;
    }

    public WorkflowExecutionBuilder appName(String appName) {
      this.appName = appName;
      return this;
    }

    public WorkflowExecutionBuilder envName(String envName) {
      this.envName = envName;
      return this;
    }

    public WorkflowExecutionBuilder envType(EnvironmentType envType) {
      this.envType = envType;
      return this;
    }

    public WorkflowExecutionBuilder workflowType(WorkflowType workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public WorkflowExecutionBuilder status(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public WorkflowExecutionBuilder graph(Graph graph) {
      this.graph = graph;
      return this;
    }

    public WorkflowExecutionBuilder executionNode(GraphNode executionNode) {
      this.executionNode = executionNode;
      return this;
    }

    public WorkflowExecutionBuilder errorStrategy(ErrorStrategy errorStrategy) {
      this.errorStrategy = errorStrategy;
      return this;
    }

    public WorkflowExecutionBuilder name(String name) {
      this.name = name;
      return this;
    }

    public WorkflowExecutionBuilder total(int total) {
      this.total = total;
      return this;
    }

    public WorkflowExecutionBuilder breakdown(CountsByStatuses breakdown) {
      this.breakdown = breakdown;
      return this;
    }

    public WorkflowExecutionBuilder executionArgs(ExecutionArgs executionArgs) {
      this.executionArgs = executionArgs;
      return this;
    }

    public WorkflowExecutionBuilder serviceExecutionSummaries(List<ElementExecutionSummary> serviceExecutionSummaries) {
      this.serviceExecutionSummaries = serviceExecutionSummaries;
      return this;
    }

    public WorkflowExecutionBuilder uuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public WorkflowExecutionBuilder statusInstanceBreakdownMap(
        LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap) {
      this.statusInstanceBreakdownMap = statusInstanceBreakdownMap;
      return this;
    }

    public WorkflowExecutionBuilder appId(String appId) {
      this.appId = appId;
      return this;
    }

    public WorkflowExecutionBuilder createdBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public WorkflowExecutionBuilder startTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public WorkflowExecutionBuilder endTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public WorkflowExecutionBuilder createdAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public WorkflowExecutionBuilder triggeredBy(EmbeddedUser triggeredBy) {
      this.triggeredBy = triggeredBy;
      return this;
    }

    public WorkflowExecutionBuilder pipelineSummary(PipelineSummary pipelineSummary) {
      this.pipelineSummary = pipelineSummary;
      return this;
    }

    public WorkflowExecutionBuilder infraMappingSummary(Map<String, InfraMappingSummary> infraMappingSummary) {
      this.infraMappingSummary = infraMappingSummary;
      return this;
    }

    public WorkflowExecutionBuilder serviceIds(List<String> serviceIds) {
      this.serviceIds = serviceIds;
      return this;
    }

    public WorkflowExecutionBuilder envIds(List<String> envIds) {
      this.envIds = envIds;
      return this;
    }

    public WorkflowExecutionBuilder infraMappingIds(List<String> infraMappingIds) {
      this.infraMappingIds = infraMappingIds;
      return this;
    }

    public WorkflowExecutionBuilder buildExecutionSummaries(List<BuildExecutionSummary> buildExecutionSummaries) {
      this.buildExecutionSummaries = buildExecutionSummaries;
      return this;
    }

    public WorkflowExecutionBuilder orchestrationWorkflowType(OrchestrationWorkflowType orchestrationWorkflowType) {
      this.orchestrationWorkflowType = orchestrationWorkflowType;
      return this;
    }

    public WorkflowExecutionBuilder pipelineExecutionId(String pipelineExecutionId) {
      this.pipelineExecutionId = pipelineExecutionId;
      return this;
    }

    public WorkflowExecutionBuilder pipelineExecution(PipelineExecution pipelineExecution) {
      this.pipelineExecution = pipelineExecution;
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
          .workflowId(workflowId)
          .stateMachineId(stateMachineId)
          .envId(envId)
          .appName(appName)
          .envName(envName)
          .envType(envType)
          .workflowType(workflowType)
          .status(status)
          .graph(graph)
          .executionNode(executionNode)
          .errorStrategy(errorStrategy)
          .name(name)
          .total(total)
          .breakdown(breakdown)
          .executionArgs(executionArgs)
          .serviceExecutionSummaries(serviceExecutionSummaries)
          .uuid(uuid)
          .statusInstanceBreakdownMap(statusInstanceBreakdownMap)
          .appId(appId)
          .createdBy(createdBy)
          .startTs(startTs)
          .endTs(endTs)
          .createdAt(createdAt)
          .triggeredBy(triggeredBy)
          .pipelineSummary(pipelineSummary)
          .infraMappingSummary(infraMappingSummary)
          .serviceIds(serviceIds)
          .envIds(envIds)
          .infraMappingIds(infraMappingIds)
          .buildExecutionSummaries(buildExecutionSummaries)
          .orchestrationWorkflowType(orchestrationWorkflowType)
          .pipelineExecutionId(pipelineExecutionId);
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
      workflowExecution.setTriggeredBy(triggeredBy);
      workflowExecution.setPipelineSummary(pipelineSummary);
      workflowExecution.setServiceIds(serviceIds);
      workflowExecution.setEnvIds(envIds);
      workflowExecution.setBuildExecutionSummaries(buildExecutionSummaries);
      workflowExecution.setOrchestrationType(orchestrationWorkflowType);
      workflowExecution.setPipelineExecutionId(pipelineExecutionId);
      workflowExecution.setPipelineExecution(pipelineExecution);
      workflowExecution.setInfraMappingIds(infraMappingIds);
      return workflowExecution;
    }
  }
}

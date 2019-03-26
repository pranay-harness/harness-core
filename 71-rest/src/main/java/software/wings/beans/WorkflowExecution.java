/**
 *
 */

package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

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
import lombok.Builder;
import lombok.Builder.Default;
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
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateMachine;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * The Class WorkflowExecution.
 */
@Data
@Builder
@Entity(value = "workflowExecutions", noClassnameStored = true)
@Indexes(@Index(options = @IndexOptions(name = "search"), fields = { @Field("workflowId")
                                                                     , @Field("status") }))
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowExecution implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware, KeywordsAware {
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
  public static final String STATE_MACHINE_KEY = "stateMachine";
  public static final String STATE_MACHINE_ID_KEY = "stateMachineId";
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
  @JsonIgnore private StateMachine stateMachine;
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

  @Default
  @JsonIgnore
  @SchemaIgnore
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  public String normalizedName() {
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
}

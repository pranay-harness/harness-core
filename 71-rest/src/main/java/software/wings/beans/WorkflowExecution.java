/**
 *
 */

package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.utils.IndexType;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs.ExecutionArgsKeys;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.concurrency.ConcurrencyStrategy;
import software.wings.beans.entityinterface.KeywordsAware;
import software.wings.beans.execution.WorkflowExecutionInfo;
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
import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * The Class WorkflowExecution.
 */
@Data
@Builder
@FieldNameConstants(innerTypeName = "WorkflowExecutionKeys")
@Entity(value = "workflowExecutions", noClassnameStored = true)
@HarnessEntity(exportable = false)
@Indexes({
  @Index(options = @IndexOptions(name = "search"),
      fields = { @Field(WorkflowExecutionKeys.workflowId)
                 , @Field(WorkflowExecutionKeys.status) })
  ,
      @Index(options = @IndexOptions(name = "app_pipExecutionId_createdAt"), fields = {
        @Field(WorkflowExecutionKeys.appId)
        , @Field(WorkflowExecutionKeys.pipelineExecutionId),
            @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "service_guard"), fields = {
        @Field(WorkflowExecutionKeys.appId), @Field(value = WorkflowExecutionKeys.startTs)
      }), @Index(options = @IndexOptions(name = "lastDeployedSearch"), fields = {
        @Field(WorkflowExecutionKeys.appId)
        , @Field(WorkflowExecutionKeys.status), @Field(WorkflowExecutionKeys.workflowId),
            @Field(WorkflowExecutionKeys.infraMappingIds),
            @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "appId_endTs", background = true), fields = {
        @Field(WorkflowExecutionKeys.appId), @Field(value = WorkflowExecutionKeys.endTs)
      }), @Index(options = @IndexOptions(name = "pipelineExecutionId", background = true), fields = {
        @Field(WorkflowExecutionKeys.pipelineExecutionId)
      }), @Index(options = @IndexOptions(name = "lastInfraMappingSearch"), fields = {
        @Field(WorkflowExecutionKeys.appId)
        , @Field(WorkflowExecutionKeys.workflowType), @Field(WorkflowExecutionKeys.status),
            @Field(WorkflowExecutionKeys.infraMappingIds),
            @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "accountId_endTs", background = true), fields = {
        @Field(WorkflowExecutionKeys.accountId), @Field(value = WorkflowExecutionKeys.endTs, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "accountId_createdAt2"), fields = {
        @Field(WorkflowExecutionKeys.accountId), @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "workflowExecutionMonitor", background = true), fields = {
        @Field(WorkflowExecutionKeys.status), @Field(WorkflowExecutionKeys.nextIteration)
      }), @Index(options = @IndexOptions(name = "accountId_pipExecutionId_createdAt"), fields = {
        @Field(WorkflowExecutionKeys.accountId)
        , @Field(WorkflowExecutionKeys.pipelineExecutionId),
            @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "searchByServiceIds"), fields = {
        @Field(WorkflowExecutionKeys.appId)
        , @Field(WorkflowExecutionKeys.workflowId), @Field(WorkflowExecutionKeys.status),
            @Field(WorkflowExecutionKeys.serviceIds),
            @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "accountId_tags_createdAt"), fields = {
        @Field(WorkflowExecutionKeys.accountId)
        , @Field("tags.name"), @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      }), @Index(options = @IndexOptions(name = "accountId_appId_tags_createdAt"), fields = {
        @Field(WorkflowExecutionKeys.accountId)
        , @Field(WorkflowExecutionKeys.appId), @Field("tags.name"),
            @Field(value = WorkflowExecutionKeys.createdAt, type = IndexType.DESC)
      })
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowExecution
    implements PersistentRegularIterable, UuidAware, CreatedAtAware, CreatedByAware, KeywordsAware, AccountAccess {
  // TODO: Determine the right expiry duration for workflow exceptions
  public static final Duration EXPIRY = Duration.ofDays(7);

  @Id @NotNull(groups = {Update.class}) private String uuid;
  @Indexed @NotNull protected String appId;
  private EmbeddedUser createdBy;
  @Indexed private long createdAt;
  @Indexed private String accountId;

  private String workflowId;

  private String stateMachineId;
  @JsonIgnore private StateMachine stateMachine;
  private String envId;
  private List<String> envIds;
  private List<String> workflowIds;
  private List<String> cloudProviderIds;
  @Indexed private List<String> serviceIds;
  @Indexed private List<String> infraMappingIds;
  @Indexed private List<String> infraDefinitionIds;
  private String appName;
  private String envName;
  private EnvironmentType envType;
  private WorkflowType workflowType;
  @Indexed private ExecutionStatus status;
  @Transient private Graph graph;

  @Transient private GraphNode executionNode; // used for workflow details.
  private PipelineExecution pipelineExecution; // used for pipeline details.

  private String pipelineExecutionId;
  private String stageName;
  private ErrorStrategy errorStrategy;

  private String name;
  private String releaseNo;
  private int total;
  private CountsByStatuses breakdown;

  private ExecutionArgs executionArgs;
  private List<ElementExecutionSummary> serviceExecutionSummaries;
  private LinkedHashMap<ExecutionStatus, StatusInstanceBreakdown> statusInstanceBreakdownMap;

  private Long startTs;
  private Long rollbackStartTs;
  private Long endTs;
  // Pre-calculated difference from endTs and startTs for indexing purposes
  private Long duration;
  private Long rollbackDuration;

  private EmbeddedUser triggeredBy;

  private PipelineSummary pipelineSummary;

  private List<EnvSummary> environments;

  private List<EnvSummary> deployedEnvironments;
  private List<String> deployedServices;
  private List<String> deployedCloudProviders;

  private List<BuildExecutionSummary> buildExecutionSummaries;

  private OrchestrationWorkflowType orchestrationType;

  private boolean isBaseline;

  private String deploymentTriggerId;

  private List<Artifact> artifacts;

  private Set<String> keywords;
  private boolean onDemandRollback;
  private boolean useSweepingOutputs;
  // Information of original execution is this is for OnDemand Rollback
  private WorkflowExecutionInfo originalExecution;

  private HelmExecutionSummary helmExecutionSummary;
  private List<AwsLambdaExecutionSummary> awsLambdaExecutionSummaries;
  private ConcurrencyStrategy concurrencyStrategy;

  // For pipeline resume.
  //
  // pipelineResumeId is the pipeline execution id of the very first execution.
  // It makes getting the history very simple. Just filter by the same
  // pipelineResumeId and sort by createdAt.
  @Indexed private String pipelineResumeId;
  // latestPipelineResume is true only for the latest resumed execution.
  // It is required to make the list execution call efficient to fetch only
  // the latest execution.
  private boolean latestPipelineResume;

  private Long nextIteration;
  private List<NameValuePair> tags;

  @Default
  @JsonIgnore
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
                .format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"));
    }
    return name + dateSuffix;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @UtilityClass
  public static final class WorkflowExecutionKeys {
    public static final String executionArgs_pipelinePhaseElementId =
        executionArgs + "." + ExecutionArgsKeys.pipelinePhaseElementId;
  }
}

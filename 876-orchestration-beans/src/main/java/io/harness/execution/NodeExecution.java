package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.interrupts.InterruptEffect;
import io.harness.logging.UnitProgress;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.execution.skip.SkipInfo;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.tasks.ProgressData;
import io.harness.timeout.TimeoutDetails;

import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Data
@Builder
@FieldNameConstants(innerTypeName = "NodeExecutionKeys")
@Entity(value = "nodeExecutions", noClassnameStored = true)
@Document("nodeExecutions")
@TypeAlias("nodeExecution")
public final class NodeExecution implements PersistentEntity, UuidAware {
  // Immutable
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  @NotNull Ambiance ambiance;
  @NotNull PlanNodeProto node;
  @NotNull ExecutionMode mode;
  @Wither @FdIndex @CreatedDate Long createdAt;
  private Long startTs;
  private Long endTs;
  private Duration initialWaitDuration;

  // Resolved StepParameters stored just before invoking step.
  org.bson.Document resolvedStepParameters;
  org.bson.Document resolvedStepInputs;

  // For Wait Notify
  String notifyId;

  // Relationships
  String parentId;
  String nextId;
  String previousId;

  // Mutable
  @Wither @LastModifiedDate Long lastUpdatedAt;
  Status status;
  @Version Long version;

  @Singular List<ExecutableResponse> executableResponses;
  @Singular private List<InterruptEffect> interruptHistories;
  FailureInfo failureInfo;
  SkipInfo skipInfo;
  NodeRunInfo nodeRunInfo;

  // Retries
  @Singular List<String> retryIds;
  boolean oldRetry;

  // Timeout
  List<String> timeoutInstanceIds;
  TimeoutDetails timeoutDetails;

  List<StepOutcomeRef> outcomeRefs;

  Map<String, List<ProgressData>> progressDataMap;

  @Singular List<UnitProgress> unitProgresses;

  AdviserResponse adviserResponse;
  // Timeouts for advisers
  List<String> adviserTimeoutInstanceIds;
  TimeoutDetails adviserTimeoutDetails;

  public boolean isChildSpawningMode() {
    return mode == ExecutionMode.CHILD || mode == ExecutionMode.CHILDREN || mode == ExecutionMode.CHILD_CHAIN;
  }

  public boolean isTaskSpawningMode() {
    return mode == ExecutionMode.TASK || mode == ExecutionMode.TASK_CHAIN;
  }

  public ExecutableResponse obtainLatestExecutableResponse() {
    if (isEmpty(executableResponses)) {
      return null;
    }
    return executableResponses.get(executableResponses.size() - 1);
  }

  @UtilityClass
  public static class NodeExecutionKeys {
    public static final String planExecutionId = NodeExecutionKeys.ambiance + "."
        + "planExecutionId";

    public static final String planNodeId = NodeExecutionKeys.node + "."
        + "uuid";
    public static final String planNodeIdentifier = NodeExecutionKeys.node + "."
        + "identifier";
  }

  public static class NodeExecutionBuilder {
    public NodeExecutionBuilder resolvedStepParameters(StepParameters stepParameters) {
      this.resolvedStepParameters = RecastOrchestrationUtils.toDocument(stepParameters);
      return this;
    }

    public NodeExecutionBuilder resolvedStepParameters(String jsonString) {
      this.resolvedStepParameters = RecastOrchestrationUtils.toDocumentFromJson(jsonString);
      return this;
    }

    public NodeExecutionBuilder resolvedStepInputs(String jsonString) {
      this.resolvedStepInputs = RecastOrchestrationUtils.toDocumentFromJson(jsonString);
      return this;
    }
  }

  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_planNodeId_idx")
                 .field(NodeExecutionKeys.planExecutionId)
                 .field(NodeExecutionKeys.planNodeId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_planNodeIdentifier_idx")
                 .field(NodeExecutionKeys.planExecutionId)
                 .field(NodeExecutionKeys.planNodeIdentifier)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_oldRetry_idx")
                 .field(NodeExecutionKeys.planExecutionId)
                 .field(NodeExecutionKeys.oldRetry)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_notifyId_idx")
                 .field(NodeExecutionKeys.planExecutionId)
                 .field(NodeExecutionKeys.notifyId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_status_idx")
                 .field(NodeExecutionKeys.planExecutionId)
                 .field(NodeExecutionKeys.status)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("planExecutionId_parentId_status_idx")
                 .field(NodeExecutionKeys.planExecutionId)
                 .field(NodeExecutionKeys.parentId)
                 .field(NodeExecutionKeys.status)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("parentId_status_idx")
                 .field(NodeExecutionKeys.parentId)
                 .field(NodeExecutionKeys.status)
                 .build())
        .build();
  }
}

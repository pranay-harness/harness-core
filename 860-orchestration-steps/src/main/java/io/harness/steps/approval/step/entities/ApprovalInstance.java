package io.harness.steps.approval.step.entities;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalStepParameters;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.timeout.TimeoutParameters;
import io.harness.yaml.core.timeout.Timeout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ApprovalInstanceKeys")
@Document("approvalInstances")
@Entity(value = "approvalInstances", noClassnameStored = true)
@Persistent
public abstract class ApprovalInstance implements PersistentEntity, PersistentRegularIterable {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("status_deadline")
                 .field(ApprovalInstanceKeys.status)
                 .field(ApprovalInstanceKeys.deadline)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String id;

  @NotEmpty String accountId;
  @NotEmpty String orgIdentifier;
  @NotEmpty String projectIdentifier;

  @NotNull String planExecutionId;
  @FdIndex @NotNull String nodeExecutionId;

  @NotNull ApprovalType type;
  @NotNull ApprovalStatus status;
  String approvalMessage;
  boolean includePipelineExecutionHistory;
  long deadline;
  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;

  long nextIteration;

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }

  private Map<String, String> logContextMap() {
    Map<String, String> logContext = new HashMap<>();
    logContext.put("approvalInstanceId", id);
    logContext.put("approvalType", type.getDisplayName());
    logContext.put(ApprovalInstanceKeys.accountId, accountId);
    logContext.put(ApprovalInstanceKeys.orgIdentifier, orgIdentifier);
    logContext.put(ApprovalInstanceKeys.projectIdentifier, projectIdentifier);
    logContext.put(ApprovalInstanceKeys.planExecutionId, planExecutionId);
    logContext.put(ApprovalInstanceKeys.nodeExecutionId, nodeExecutionId);
    return logContext;
  }

  protected void updateFromStepParameters(Ambiance ambiance, ApprovalStepParameters stepParameters) {
    if (stepParameters == null) {
      return;
    }

    setId(generateUuid());
    setAccountId(AmbianceUtils.getAccountId(ambiance));
    setOrgIdentifier(AmbianceUtils.getOrgIdentifier(ambiance));
    setProjectIdentifier(AmbianceUtils.getProjectIdentifier(ambiance));
    setPlanExecutionId(ambiance.getPlanExecutionId());
    setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    setType(stepParameters.getApprovalType());
    setStatus(ApprovalStatus.WAITING);
    setApprovalMessage((String) stepParameters.getApprovalMessage().fetchFinalValue());
    setIncludePipelineExecutionHistory((boolean) stepParameters.getIncludePipelineExecutionHistory().fetchFinalValue());
    setDeadline(calculateDeadline(stepParameters.getTimeout()));
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return this.nextIteration;
  }

  @Override
  public String getUuid() {
    return id;
  }

  private static long calculateDeadline(ParameterField<String> timeoutField) {
    if (ParameterField.isNull(timeoutField)) {
      return TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
    } else if (timeoutField.isExpression()) {
      throw new InvalidArgumentsException(String.format("Invalid timeout: '%s'", timeoutField.fetchFinalValue()));
    } else if (timeoutField.getValue() == null) {
      return TimeoutParameters.DEFAULT_TIMEOUT_IN_MILLIS;
    }

    Timeout timeout = Timeout.fromString(timeoutField.getValue());
    if (timeout == null) {
      throw new InvalidArgumentsException(String.format("Invalid timeout: '%s'", timeoutField.fetchFinalValue()));
    }
    return System.currentTimeMillis() + timeout.getTimeoutInMillis();
  }
}

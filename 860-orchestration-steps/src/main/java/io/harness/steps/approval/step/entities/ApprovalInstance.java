package io.harness.steps.approval.step.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.exception.InvalidArgumentsException;
import io.harness.persistence.PersistentEntity;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.ApprovalStepParameters;
import io.harness.steps.approval.step.beans.ApprovalInstanceDetailsDTO;
import io.harness.steps.approval.step.beans.ApprovalInstanceResponseDTO;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.timeout.TimeoutParameters;
import io.harness.yaml.core.timeout.Timeout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "ApprovalInstanceKeys")
@Document("approvalInstances")
@Entity(value = "approvalInstances", noClassnameStored = true)
@Persistent
public abstract class ApprovalInstance implements PersistentEntity {
  @Id @org.mongodb.morphia.annotations.Id String id;

  @NotNull private String planExecutionId;
  @NotNull private String nodeExecutionId;

  @NotNull ApprovalType type;
  @NotNull ApprovalStatus status;
  String approvalMessage;
  boolean includePipelineExecutionHistory;
  long deadline;

  @CreatedDate Long createdAt;
  @LastModifiedDate Long lastModifiedAt;
  @Version Long version;

  protected void updateFromStepParameters(Ambiance ambiance, ApprovalStepParameters stepParameters) {
    if (stepParameters == null) {
      return;
    }

    setId(generateUuid());
    setPlanExecutionId(ambiance.getPlanExecutionId());
    setNodeExecutionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    setStatus(ApprovalStatus.WAITING);
    setApprovalMessage((String) stepParameters.getApprovalMessage().fetchFinalValue());
    setIncludePipelineExecutionHistory((boolean) stepParameters.getIncludePipelineExecutionHistory().fetchFinalValue());
    setDeadline(calculateDeadline(stepParameters.getTimeout()));
  }

  public ApprovalInstanceResponseDTO toApprovalInstanceResponseDTO() {
    return ApprovalInstanceResponseDTO.builder()
        .id(generateUuid())
        .type(type)
        .status(ApprovalStatus.WAITING)
        .approvalMessage(approvalMessage)
        .includePipelineExecutionHistory(includePipelineExecutionHistory)
        .deadline(deadline)
        .details(toApprovalInstanceDetailsDTO())
        .createdAt(createdAt)
        .lastModifiedAt(lastModifiedAt)
        .build();
  }

  public ApprovalInstanceDetailsDTO toApprovalInstanceDetailsDTO() {
    return null;
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

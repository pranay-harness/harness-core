package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CreatedByType;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.WorkflowExecution;

@OwnedBy(CDC)
@Value
@Builder
public class TriggeredByMetadata {
  CreatedByType type;
  String name;
  String email;

  static TriggeredByMetadata fromWorkflowExecution(WorkflowExecution workflowExecution) {
    if (workflowExecution == null || workflowExecution.getTriggeredBy() == null) {
      return null;
    }

    if (workflowExecution.getCreatedByType() == CreatedByType.API_KEY
        || workflowExecution.getCreatedByType() == CreatedByType.TRIGGER) {
      return TriggeredByMetadata.builder()
          .type(workflowExecution.getCreatedByType())
          .name(workflowExecution.getTriggeredBy().getName())
          .build();
    } else if (workflowExecution.getDeploymentTriggerId() != null) {
      return TriggeredByMetadata.builder()
          .type(CreatedByType.TRIGGER)
          .name(workflowExecution.getTriggeredBy().getName())
          .build();
    }

    return TriggeredByMetadata.builder()
        .type(CreatedByType.USER)
        .name(workflowExecution.getTriggeredBy().getName())
        .email(workflowExecution.getTriggeredBy().getEmail())
        .build();
  }
}

package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowExecutionKeys")
public class QLWorkflowExecution implements QLExecution {
  private String id;
  private Long queuedTime;
  private Long startTime;
  private Long endTime;
  private QLExecutionStatus status;
}

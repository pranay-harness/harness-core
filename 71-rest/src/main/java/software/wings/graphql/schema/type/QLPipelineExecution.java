package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPipelineExecution implements QLExecution {
  private String id;
  private Long queuedTime;
  private Long startTime;
  private Long endTime;
  private QLExecutionStatus status;
}

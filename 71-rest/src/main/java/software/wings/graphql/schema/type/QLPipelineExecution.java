package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class QLPipelineExecution implements QLExecution {
  private String id;
  private long queuedTime;
  private long startTime;
  private long endTime;
  private QLExecutionStatus status;
}

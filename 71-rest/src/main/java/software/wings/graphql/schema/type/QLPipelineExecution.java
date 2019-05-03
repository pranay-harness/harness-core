package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder
public class QLPipelineExecution implements QLExecution {
  private String id;
  private ZonedDateTime triggeredAt;
  private ZonedDateTime startedAt;
  private ZonedDateTime endedAt;
  private QLExecutionStatus status;
}

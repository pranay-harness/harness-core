package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

import java.time.ZonedDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPipelineExecutionKeys")
public class QLPipelineExecution implements QLExecution {
  private String id;
  private ZonedDateTime triggeredAt;
  private ZonedDateTime startedAt;
  private ZonedDateTime endedAt;
  private QLExecutionStatus status;
}

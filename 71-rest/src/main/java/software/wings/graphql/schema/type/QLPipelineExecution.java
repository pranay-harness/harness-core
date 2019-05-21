package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.OffsetDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPipelineExecutionKeys")
@Scope(ResourceType.APPLICATION)
public class QLPipelineExecution implements QLExecution, QLCause {
  private String id;
  private OffsetDateTime createdAt;
  private OffsetDateTime startedAt;
  private OffsetDateTime endedAt;
  private QLExecutionStatus status;
  private QLCause cause;
  private String notes;
}

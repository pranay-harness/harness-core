package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.ZonedDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowExecutionKeys")
@Scope(ResourceType.APPLICATION)
public class QLWorkflowExecution implements QLExecution {
  private String id;
  private ZonedDateTime createdAt;
  private ZonedDateTime startedAt;
  private ZonedDateTime endedAt;
  private QLExecutionStatus status;
  private QLCause cause;
}

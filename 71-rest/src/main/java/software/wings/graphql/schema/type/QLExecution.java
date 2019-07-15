package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Scope(ResourceType.APPLICATION)
public interface QLExecution extends QLObject {
  Long getCreatedAt();
  Long getStartedAt();
  Long getEndedAt();
  QLExecutionStatus getStatus();
  QLCause getCause();
}

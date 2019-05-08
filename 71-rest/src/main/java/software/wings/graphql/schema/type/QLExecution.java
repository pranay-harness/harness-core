package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.ZonedDateTime;

@Scope(ResourceType.APPLICATION)
public interface QLExecution extends QLObject {
  ZonedDateTime getCreatedAt();
  ZonedDateTime getStartedAt();
  ZonedDateTime getEndedAt();
  QLExecutionStatus getStatus();
  QLCause getCause();
}

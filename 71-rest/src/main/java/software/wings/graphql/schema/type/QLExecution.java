package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.OffsetDateTime;

@Scope(ResourceType.APPLICATION)
public interface QLExecution extends QLObject {
  OffsetDateTime getCreatedAt();
  OffsetDateTime getStartedAt();
  OffsetDateTime getEndedAt();
  QLExecutionStatus getStatus();
  QLCause getCause();
}

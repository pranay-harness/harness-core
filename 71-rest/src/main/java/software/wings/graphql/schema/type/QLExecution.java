package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Scope(ResourceType.APPLICATION)
public interface QLExecution extends QLObject {
  Long getCreatedAt();
  Long getStartedAt();
  Long getEndedAt();
  QLExecutionStatus getStatus();
  QLCause getCause();
}

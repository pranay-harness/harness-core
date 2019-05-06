package software.wings.graphql.schema.type;

import java.time.ZonedDateTime;

public interface QLExecution extends QLObject {
  ZonedDateTime getCreatedAt();
  ZonedDateTime getStartedAt();
  ZonedDateTime getEndedAt();
  QLExecutionStatus getStatus();
  QLCause getCause();
}

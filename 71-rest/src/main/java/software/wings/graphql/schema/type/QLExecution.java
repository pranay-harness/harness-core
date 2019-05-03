package software.wings.graphql.schema.type;

import java.time.ZonedDateTime;

public interface QLExecution extends QLObject {
  ZonedDateTime getTriggeredAt();
  ZonedDateTime getStartedAt();
  ZonedDateTime getEndedAt();
  QLExecutionStatus getStatus();
}

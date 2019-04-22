package software.wings.graphql.schema.type;

import io.harness.beans.ExecutionStatus;

public interface QLExecution extends QLObject {
  long getQueuedTime();
  long getStartTime();
  long getEndTime();
  ExecutionStatus getStatus();
}

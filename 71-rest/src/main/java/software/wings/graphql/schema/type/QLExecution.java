package software.wings.graphql.schema.type;

public interface QLExecution extends QLObject {
  long getQueuedTime();
  long getStartTime();
  long getEndTime();
  QLExecutionStatus getStatus();
}

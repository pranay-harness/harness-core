package software.wings.graphql.schema.type;

public interface QLExecution extends QLObject {
  Long getQueuedTime();
  Long getStartTime();
  Long getEndTime();
  QLExecutionStatus getStatus();
}

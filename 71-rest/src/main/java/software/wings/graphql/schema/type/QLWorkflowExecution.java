package software.wings.graphql.schema.type;

import io.harness.beans.ExecutionStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLWorkflowExecution implements QLObject, BaseInfo {
  String id;
  String name;
  ExecutionStatus status;
  long startTime;
  long endTime;
  String debugInfo;
}

package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLExecutionStatus {
  ABORTED,
  ERROR,
  FAILED,
  PAUSED,
  QUEUED,
  RESUMED,
  RUNNING,
  SUCCESS,
  WAITING,
  SKIPPED,
  REJECTED,
  EXPIRED
}

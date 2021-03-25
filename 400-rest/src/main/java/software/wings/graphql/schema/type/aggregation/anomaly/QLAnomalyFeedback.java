package software.wings.graphql.schema.type.aggregation.anomaly;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLAnomalyFeedback {
  TRUE_ANOMALY,
  FALSE_ANOMALY,
  NOT_RESPONDED
}

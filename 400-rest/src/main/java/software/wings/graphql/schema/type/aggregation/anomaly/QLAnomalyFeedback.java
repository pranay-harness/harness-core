package software.wings.graphql.schema.type.aggregation.anomaly;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public enum QLAnomalyFeedback {
  TRUE_ANOMALY,
  FALSE_ANOMALY,
  NOT_RESPONDED
}

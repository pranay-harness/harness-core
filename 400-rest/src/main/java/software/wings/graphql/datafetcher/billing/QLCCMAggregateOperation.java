package software.wings.graphql.datafetcher.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public enum QLCCMAggregateOperation {
  SUM,
  MIN,
  MAX,
  AVG,
  COUNT
}

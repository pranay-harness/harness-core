package software.wings.graphql.datafetcher.billing;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL) public enum QLCCMAggregateOperation { SUM, MIN, MAX, AVG, COUNT }

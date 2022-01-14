package software.wings.graphql.datafetcher;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface BaseStatsDataFetcher {
  String getEntityType();
}

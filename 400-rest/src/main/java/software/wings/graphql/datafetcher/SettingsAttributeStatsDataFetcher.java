package software.wings.graphql.datafetcher;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@OwnedBy(DX)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public abstract class SettingsAttributeStatsDataFetcher<A, F, G, S> extends RealTimeStatsDataFetcher<A, F, G, S> {}

package software.wings.graphql.datafetcher.anomaly;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._375_CE_GRAPHQL)
public class AnomalyDataHelper {
  public static double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }
}

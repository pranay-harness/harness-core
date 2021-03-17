package software.wings.graphql.datafetcher.anomaly;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
@TargetModule(Module._380_CG_GRAPHQL)
public class AnomalyDataHelper {
  public static double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }
}

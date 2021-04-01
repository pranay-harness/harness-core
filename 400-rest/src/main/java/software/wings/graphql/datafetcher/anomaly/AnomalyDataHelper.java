package software.wings.graphql.datafetcher.anomaly;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CE)
public class AnomalyDataHelper {
  public static double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }
}

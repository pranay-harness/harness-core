package software.wings.graphql.datafetcher.anomaly;

public class AnomalyDataHelper {
  public static double getRoundedDoubleValue(double value) {
    return Math.round(value * 100D) / 100D;
  }
}

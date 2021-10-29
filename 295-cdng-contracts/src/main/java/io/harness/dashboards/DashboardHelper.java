package io.harness.dashboards;

public class DashboardHelper {
  public static final double MAX_VALUE = truncate(Double.MAX_VALUE);

  public static double truncate(double input) {
    return Math.floor(input * 100) / 100;
  }
}

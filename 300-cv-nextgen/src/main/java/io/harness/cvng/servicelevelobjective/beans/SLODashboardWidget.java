package io.harness.cvng.servicelevelobjective.beans;

import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import lombok.Builder;
import lombok.Value;
@Value
@Builder
public class SLODashboardWidget {
  String title;
  String monitoredServiceIdentifier;
  String monitoredServiceName;
  String healthSourceIdentifier;
  String healthSourceName;
  List<NGTag> tags;
  ServiceLevelIndicatorType type;
  BurnRate burnRate;
  int timeRemainingDays;
  double errorBudgetRemainingPercentage;
  List<Point> errorBudgetBurndown;
  List<Point> sloPerformanceTrend;
  @Value
  @Builder
  public static class BurnRate {
    double currentRatePercentage; // rate per day for the current period
  }
  @Value
  @Builder
  public static class Point {
    long timestamp;
    double value;
  }
  @Value
  @Builder
  public static class SLOGraphData {
    double errorBudgetRemainingPercentage;
    List<Point> errorBudgetBurndown;
    List<Point> sloPerformanceTrend;
    public double errorBudgetSpentPercentage() {
      return 100 - errorBudgetRemainingPercentage;
    }
  }
}

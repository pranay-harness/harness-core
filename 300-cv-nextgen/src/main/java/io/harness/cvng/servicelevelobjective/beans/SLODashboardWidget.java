package io.harness.cvng.servicelevelobjective.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SLODashboardWidget {
  @NotNull String sloIdentifier;
  @NotNull String title;
  @NotNull String monitoredServiceIdentifier;
  @NotNull String monitoredServiceName;
  @NotNull String healthSourceIdentifier;
  @NotNull String healthSourceName;
  @NotNull String serviceIdentifier;
  @NotNull String environmentIdentifier;
  @NotNull String environmentName;
  @NotNull String serviceName;
  Map<String, String> tags;
  @NotNull ServiceLevelIndicatorType type;
  @NotNull BurnRate burnRate;
  @NotNull int timeRemainingDays;
  @NotNull double errorBudgetRemainingPercentage;
  @NotNull
  public ErrorBudgetRisk getErrorBudgetRisk() {
    return ErrorBudgetRisk.getFromPercentage(errorBudgetRemainingPercentage);
  }
  @NotNull int errorBudgetRemaining;
  @NotNull int totalErrorBudget;
  @NotNull SLOTargetType sloTargetType;
  @NotNull int currentPeriodLengthDays;
  @NotNull long currentPeriodStartTime;
  @NotNull long currentPeriodEndTime;
  @NotNull double sloTargetPercentage;
  @NotNull List<Point> errorBudgetBurndown;
  @NotNull List<Point> sloPerformanceTrend;
  @NotNull boolean isRecalculatingSLI;
  @Value
  @Builder
  public static class BurnRate {
    @NotNull double currentRatePercentage; // rate per day for the current period
  }
  @Value
  @Builder
  public static class Point {
    long timestamp;
    double value;
  }

  public static SLODashboardWidgetBuilder withGraphData(SLOGraphData sloGraphData) {
    return SLODashboardWidget.builder()
        .isRecalculatingSLI(sloGraphData.isRecalculatingSLI())
        .errorBudgetRemaining(sloGraphData.getErrorBudgetRemaining())
        .errorBudgetRemainingPercentage(sloGraphData.getErrorBudgetRemainingPercentage())
        .errorBudgetBurndown(sloGraphData.getErrorBudgetBurndown())
        .burnRate(BurnRate.builder().currentRatePercentage(sloGraphData.dailyBurnRate()).build())
        .sloPerformanceTrend(sloGraphData.getSloPerformanceTrend());
  }
  @Value
  @Builder
  public static class SLOGraphData {
    double errorBudgetRemainingPercentage;
    int errorBudgetRemaining;
    List<Point> errorBudgetBurndown;
    List<Point> sloPerformanceTrend;
    boolean isRecalculatingSLI;
    public double errorBudgetSpentPercentage() {
      return 100 - errorBudgetRemainingPercentage;
    }

    public double dailyBurnRate() {
      if (isEmpty(sloPerformanceTrend)) {
        return 0;
      } else {
        Instant startTime = Instant.ofEpochMilli(sloPerformanceTrend.get(0).getTimestamp());
        Instant endTime = Instant.ofEpochMilli(sloPerformanceTrend.get(sloPerformanceTrend.size() - 1).getTimestamp());
        Duration duration = Duration.between(startTime, endTime);
        if (duration.isZero()) {
          return errorBudgetSpentPercentage();
        }
        // calculating in hours to avoid boundary condition with days.
        return (errorBudgetSpentPercentage() * 24.0) / duration.toHours();
      }
    }
  }
}

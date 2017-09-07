package software.wings.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import software.wings.beans.Base;

import java.util.Map;

/**
 * Created by mike@ on 5/23/17.
 */
@Entity(value = "appdynamicsMetricDefinitions", noClassnameStored = true)
@Data
public abstract class MetricDefinition extends Base {
  @NotEmpty protected String accountId;
  @NotEmpty protected String metricId;
  @NotEmpty protected String metricName;
  @NotEmpty protected MetricType metricType;
  protected Map<ThresholdComparisonType, Threshold> thresholds;

  public void addThreshold(ThresholdComparisonType tct, Threshold threshold) {
    thresholds.put(tct, threshold);
  }

  public enum ThresholdType { ALERT_WHEN_LOWER, ALERT_WHEN_HIGHER, NO_ALERT }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Threshold {
    private ThresholdType thresholdType;
    private ThresholdComparisonType comparisonType;
    private double high;
    private double medium;

    public RiskLevel getRiskLevel(double testValue, double controlValue) {
      if (testValue == 0.0) {
        return RiskLevel.LOW;
      }

      double compareValue;
      switch (comparisonType) {
        case RATIO:
          compareValue = controlValue > 0.0 ? testValue / controlValue : 0;
          break;
        case DELTA:
          compareValue = Math.abs(controlValue - testValue);
          break;
        case ABSOLUTE:
          compareValue = testValue;
          break;

        default:
          throw new RuntimeException("Invalid comparison type: " + comparisonType);
      }
      if (compareValue == 0.0) {
        return RiskLevel.LOW;
      }

      switch (thresholdType) {
        case NO_ALERT:
          return RiskLevel.LOW;
        case ALERT_WHEN_LOWER:
          if (compareValue < medium && compareValue >= high) {
            return RiskLevel.MEDIUM;
          }

          if (compareValue < high) {
            return RiskLevel.HIGH;
          }

          return RiskLevel.LOW;

        case ALERT_WHEN_HIGHER:
          if (compareValue > medium && compareValue <= high) {
            return RiskLevel.MEDIUM;
          }

          if (compareValue > high) {
            return RiskLevel.HIGH;
          }

          return RiskLevel.LOW;

        default:
          throw new IllegalStateException("invalid thresholdType: " + thresholdType);
      }
    }
  }
}

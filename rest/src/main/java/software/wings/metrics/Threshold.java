package software.wings.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rsingh on 10/11/17.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Threshold {
  private ThresholdType thresholdType;
  private ThresholdComparisonType comparisonType;
  private double high;
  private double medium;
  private double min;

  public RiskLevel getRiskLevel(double testValue, double controlValue) {
    if (testValue == 0.0 & controlValue == 0.0) {
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

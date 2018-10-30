package software.wings.verification.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import software.wings.metrics.RiskLevel;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeatMapUnit implements Comparable<HeatMapUnit> {
  private long startTime;
  private long endTime;

  private int highRisk;
  private int mediumRisk;
  private int lowRisk;
  private int na;

  @Override
  public int compareTo(@NotNull HeatMapUnit o) {
    return (int) (this.startTime - o.startTime);
  }

  public void increment(RiskLevel riskLevel) {
    switch (riskLevel) {
      case HIGH:
        highRisk++;
        break;
      case MEDIUM:
        mediumRisk++;
        break;
      case LOW:
        lowRisk++;
        break;
      case NA:
        na++;
        break;
      default:
        throw new IllegalStateException("Invalid risklevel " + riskLevel);
    }
  }
}

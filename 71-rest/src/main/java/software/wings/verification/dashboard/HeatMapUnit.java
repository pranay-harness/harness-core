package software.wings.verification.dashboard;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.HIGH_RISK_CUTOFF;
import static software.wings.common.VerificationConstants.MEDIUM_RISK_CUTOFF;
import static software.wings.common.VerificationConstants.NO_DATA_CUTOFF;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import software.wings.metrics.RiskLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Vaibhav Tulsyan
 * 11/Oct/2018
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatMapUnit implements Comparable<HeatMapUnit> {
  private long startTime;
  private long endTime;

  private int highRisk;
  private int mediumRisk;
  private int lowRisk;

  @Builder.Default private int na = 1;

  @Builder.Default private double overallScore = -1;
  private List<Double> scoreList;

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

  public void updateOverallScore(Map<String, Double> overallMetricScores) {
    if (isNotEmpty(overallMetricScores)) {
      if (scoreList == null) {
        scoreList = new ArrayList<>();
      }
      scoreList.add(Collections.max(overallMetricScores.values()));
      overallScore = scoreList.stream().mapToDouble(val -> val).average().orElse(0.0);
      updateRisksInUnit();
    }
  }

  public void updateOverallScore(Double overallMetricScore) {
    if (scoreList == null) {
      scoreList = new ArrayList<>();
    }
    scoreList.add(overallMetricScore);
    overallScore = scoreList.stream().mapToDouble(val -> val).average().orElse(0.0);
    updateRisksInUnit();
  }

  public void updateRisksInUnit() {
    if (scoreList == null || overallScore < NO_DATA_CUTOFF) {
      highRisk = 0;
      mediumRisk = 0;
      lowRisk = 0;
      na = 1;
    } else if (overallScore > HIGH_RISK_CUTOFF) {
      highRisk = 1;
      mediumRisk = 0;
      lowRisk = 0;
      na = 0;
    } else if (overallScore < MEDIUM_RISK_CUTOFF) {
      highRisk = 0;
      mediumRisk = 0;
      lowRisk = 1;
      na = 0;
    } else {
      highRisk = 0;
      mediumRisk = 1;
      lowRisk = 0;
      na = 0;
    }
  }
}

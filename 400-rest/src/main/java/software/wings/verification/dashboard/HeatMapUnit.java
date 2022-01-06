/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.dashboard;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.common.VerificationConstants.HIGH_RISK_CUTOFF;
import static software.wings.common.VerificationConstants.MEDIUM_RISK_CUTOFF;
import static software.wings.common.VerificationConstants.NO_DATA_CUTOFF;

import software.wings.metrics.RiskLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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

  @Builder.Default private double keyTransactionScore = -1;
  private List<Double> keyTransactionScoreList;

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

  public void updateKeyTransactionScores(Map<String, Map<String, Double>> keyTransactionMetricScores) {
    if (isNotEmpty(keyTransactionMetricScores)) {
      if (keyTransactionScoreList == null) {
        keyTransactionScoreList = new ArrayList<>();
      }
      keyTransactionMetricScores.forEach((transaction, metricScores) -> {
        keyTransactionScore = updateScores(metricScores, keyTransactionScoreList);
      });
    }
  }

  private double updateScores(Map<String, Double> metricScores, List<Double> scoreListToUpdate) {
    if (isNotEmpty(metricScores)) {
      if (scoreListToUpdate == null) {
        scoreListToUpdate = new ArrayList<>();
      }
      scoreListToUpdate.add(Collections.max(metricScores.values()));
      return scoreListToUpdate.stream().mapToDouble(val -> val).average().orElse(0.0);
    }
    return -1;
  }

  public void updateOverallScore(Double overallMetricScore) {
    if (overallMetricScore == null) {
      return;
    }
    if (scoreList == null) {
      scoreList = new ArrayList<>();
    }
    if (overallMetricScore >= 0.0) {
      scoreList.add(overallMetricScore);
    }
    overallScore = scoreList.stream().mapToDouble(val -> val).average().orElse(-1.0);
    updateRisksInUnit();
  }

  public void updateKeyTransactionScore(Double keyTransactionScore) {
    if (keyTransactionScoreList == null) {
      keyTransactionScoreList = new ArrayList<>();
    }
    keyTransactionScoreList.add(keyTransactionScore);
    this.keyTransactionScore = keyTransactionScoreList.stream().mapToDouble(val -> val).average().orElse(0.0);
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

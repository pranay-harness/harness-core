package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.core.beans.params.TimeRangeParams;

import com.google.common.base.Preconditions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HistoricalTrend {
  List<RiskData> healthScores;

  @Builder
  public HistoricalTrend(int size, Instant trendStartTime, Instant trendEndTime) {
    Preconditions.checkState(size != 0, "Cannot create historical trend with 0 Health Scores");
    Preconditions.checkState(trendStartTime != null && trendEndTime != null,
        "Cannot create historical trend with start time or end time as null");

    healthScores = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      healthScores.add(RiskData.builder().riskStatus(Risk.NO_DATA).healthScore(null).build());
    }

    long windowInMillis = (trendEndTime.toEpochMilli() - trendStartTime.toEpochMilli()) / size;

    for (RiskData riskData : healthScores) {
      TimeRangeParams timeRangeParams = TimeRangeParams.builder()
                                            .startTime(trendStartTime)
                                            .endTime(trendStartTime.plusMillis(windowInMillis))
                                            .build();
      riskData.setTimeRangeParams(timeRangeParams);
      trendStartTime = trendStartTime.plusMillis(windowInMillis);
    }
  }

  public void reduceHealthScoreDataToXPoints(int x) {
    Preconditions.checkState(healthScores != null, "HealthScores cannot be null");
    Preconditions.checkState(healthScores.size() != 0 && healthScores.size() % x == 0,
        String.format("cannot group historical trend with size %s to %s points", this.healthScores.size(), x));

    List<RiskData> reducedHealthScores = new ArrayList<>();
    RiskData maxRiskData = RiskData.builder().riskStatus(Risk.NO_DATA).healthScore(null).build();
    Instant startTime = healthScores.get(0).getTimeRangeParams().getStartTime();
    for (int i = 0; i < healthScores.size(); i++) {
      if (healthScores.get(i).compareTo(maxRiskData) == 1) {
        maxRiskData.setHealthScore(healthScores.get(i).healthScore);
        maxRiskData.setRiskStatus(healthScores.get(i).riskStatus);
      }
      if ((i + 1) % x == 0) {
        Instant endTime = healthScores.get(i).getTimeRangeParams().getEndTime();
        maxRiskData.setTimeRangeParams(TimeRangeParams.builder().startTime(startTime).endTime(endTime).build());
        startTime = endTime;
        reducedHealthScores.add(maxRiskData);
        maxRiskData = RiskData.builder().riskStatus(Risk.NO_DATA).healthScore(null).build();
      }
    }
    this.setHealthScores(reducedHealthScores);
  }
}

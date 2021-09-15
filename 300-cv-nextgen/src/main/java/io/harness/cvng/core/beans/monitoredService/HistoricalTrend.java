package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.core.beans.params.TimeRangeParams;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HistoricalTrend {
  List<RiskData> healthScores;

  @Builder
  public HistoricalTrend(int size) {
    healthScores = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      healthScores.add(RiskData.builder().riskStatus(Risk.NO_DATA).healthScore(null).build());
    }
  }

  public static HistoricalTrend timeStampBuild(int size, Instant trendStartTime, Instant trendEndTime) {
    long windowInMillis = (trendEndTime.toEpochMilli() - trendStartTime.toEpochMilli()) / size;
    HistoricalTrend historicalTrend = HistoricalTrend.builder().size(size).build();

    for (RiskData riskData : historicalTrend.getHealthScores()) {
      TimeRangeParams timeRangeParams = TimeRangeParams.builder()
                                            .startTime(trendStartTime)
                                            .endTime(trendStartTime.plusMillis(windowInMillis))
                                            .build();
      riskData.setTimeRangeParams(timeRangeParams);
      trendStartTime = trendStartTime.plusMillis(windowInMillis);
    }
    return historicalTrend;
  }
}

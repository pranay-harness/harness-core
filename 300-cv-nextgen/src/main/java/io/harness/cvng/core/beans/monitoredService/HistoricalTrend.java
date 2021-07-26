package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.analysis.beans.Risk;

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
      healthScores.add(RiskData.builder().riskStatus(Risk.NO_DATA).riskValue(-2).build());
    }
  }
}

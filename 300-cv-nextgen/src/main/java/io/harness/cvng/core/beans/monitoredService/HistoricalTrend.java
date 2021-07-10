package io.harness.cvng.core.beans.monitoredService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
public class HistoricalTrend {
  List<Double> healthScores;

  @Builder
  public HistoricalTrend(int size) {
    healthScores = new ArrayList<>(Collections.nCopies(size, Double.valueOf(-1)));
  }
}

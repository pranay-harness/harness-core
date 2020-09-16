package io.harness.batch.processing.anomalydetection.models;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.TimeSeriesUtils;
import io.harness.batch.processing.anomalydetection.types.AnomalyDetectionModel;
import io.harness.batch.processing.anomalydetection.types.AnomalyType;
import lombok.Builder;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Builder
public class StatsModel {
  private static final Double RELATIVITY_THRESHOLD = 1.25;
  private static final Double ABSOLUTE_THRESHOLD = 20.0;
  private static final Double PROBABILITY_THRESHOLD = 0.995;

  public List<Anomaly> detectAnomaly(AnomalyDetectionTimeSeries data) {
    List<Double> stats = TimeSeriesUtils.getStats(data);
    Double mean = stats.get(0);
    Double standardDeviation = stats.get(1);

    List<Anomaly> anomaliesList = new ArrayList<>();

    for (Instant current : data.getTestTimePointsList()) {
      Double currentValue = data.getValue(current);

      boolean probabilityThreshold = true;
      if (standardDeviation > 0) {
        probabilityThreshold = probabilityThreshold(currentValue, mean, standardDeviation);
      }

      Anomaly currentAnomaly = Anomaly.builder()
                                   .accountId(data.getAccountId())
                                   .timeGranularity(data.getTimeGranularity())
                                   .entityId(data.getEntityId())
                                   .entityType(data.getEntityType())
                                   .clusterId(data.getClusterId())
                                   .clusterName(data.getClusterName())
                                   .cloudProvider(data.getCloudProvider())
                                   .workloadType(data.getWorkloadType())
                                   .workloadName(data.getWorkloadName())
                                   .namespace(data.getNamespace())
                                   .instant(current)
                                   .relativeThreshold(relativityThreshold(currentValue, mean))
                                   .absoluteThreshold(absoluteThreshold(currentValue, mean))
                                   .probabilisticThreshold(probabilityThreshold)
                                   .reportedBy(AnomalyDetectionModel.STATISTICAL)
                                   .build();
      if (currentValue > mean) {
        currentAnomaly.setAnomalyType(AnomalyType.SPIKE);
      } else {
        currentAnomaly.setAnomalyType(AnomalyType.DROP);
      }

      boolean isAnomaly = currentAnomaly.isRelativeThreshold() && currentAnomaly.isProbabilisticThreshold()
          && currentAnomaly.isAbsoluteThreshold();
      currentAnomaly.setAnomaly(isAnomaly);
      anomaliesList.add(currentAnomaly);
    }
    return anomaliesList;
  }

  private static boolean relativityThreshold(Double original, Double expected) {
    return original > StatsModel.RELATIVITY_THRESHOLD * expected;
  }

  private static boolean absoluteThreshold(Double original, Double expected) {
    return original > expected + StatsModel.ABSOLUTE_THRESHOLD;
  }

  private static boolean probabilityThreshold(Double original, Double mean, Double standardDeviation) {
    NormalDistribution normal = new NormalDistribution(mean, standardDeviation);
    return normal.cumulativeProbability(original) > StatsModel.PROBABILITY_THRESHOLD;
  }
}

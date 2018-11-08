package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import software.wings.metrics.RiskLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vaibhav Tulsyan
 * 24/Oct/2018
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTimeSeries implements Comparable<TransactionTimeSeries> {
  private String transactionName;
  private SortedSet<TimeSeriesOfMetric> metricTimeSeries;

  @Override
  public int compareTo(@NotNull TransactionTimeSeries o) {
    if (isEmpty(o.metricTimeSeries) && isEmpty(this.metricTimeSeries)) {
      return this.transactionName.compareTo(o.transactionName);
    }

    if (isEmpty(this.metricTimeSeries) && isNotEmpty(o.metricTimeSeries)) {
      return -1;
    }

    if (isNotEmpty(this.metricTimeSeries) && isEmpty(o.metricTimeSeries)) {
      return 1;
    }

    Map<RiskLevel, Integer> thisRiskMap = new HashMap<RiskLevel, Integer>() {
      {
        for (RiskLevel level : RiskLevel.values()) {
          put(level, 0);
        }
      }
    };
    metricTimeSeries.forEach(metric -> {
      if (metric.getRisk() != -1) {
        RiskLevel thisLevel = RiskLevel.getRiskLevel(metric.getRisk());
        thisRiskMap.put(thisLevel, thisRiskMap.get(thisLevel) + 1);
      }
    });

    Map<RiskLevel, Integer> otherRiskMap = new HashMap<RiskLevel, Integer>() {
      {
        for (RiskLevel level : RiskLevel.values()) {
          put(level, 0);
        }
      }
    };
    o.metricTimeSeries.forEach(metric -> {
      if (metric.getRisk() != -1) {
        RiskLevel thisLevel = RiskLevel.getRiskLevel(metric.getRisk());
        otherRiskMap.put(thisLevel, otherRiskMap.get(thisLevel) + 1);
      }
    });

    for (RiskLevel level : RiskLevel.values()) {
      if (!otherRiskMap.get(level).equals(thisRiskMap.get(level))) {
        return otherRiskMap.get(level) - thisRiskMap.get(level);
      }
    }

    // compare the first metric list of both and order
    if (isNotEmpty(this.metricTimeSeries.first().getTimeSeries())
        && isNotEmpty(o.metricTimeSeries.first().getTimeSeries())) {
      AtomicInteger numEmptyThis = new AtomicInteger(0);
      AtomicInteger numEmptyOther = new AtomicInteger(0);

      this.metricTimeSeries.first().getTimeSeriesMap().forEach((k, v) -> {
        if (v.getValue() == -1) {
          numEmptyThis.getAndIncrement();
        }
      });

      o.metricTimeSeries.first().getTimeSeriesMap().forEach((k, v) -> {
        if (v.getValue() == -1) {
          numEmptyOther.getAndIncrement();
        }
      });

      if (numEmptyThis.get() != numEmptyOther.get()) {
        return numEmptyThis.get() - numEmptyOther.get();
      }
    }
    return this.transactionName.compareTo(o.transactionName);
  }
}

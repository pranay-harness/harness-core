package software.wings.verification;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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

    AtomicInteger thisRisk = new AtomicInteger();
    metricTimeSeries.forEach(metric -> thisRisk.addAndGet(metric.getRisk()));

    AtomicInteger otherRisk = new AtomicInteger();
    o.metricTimeSeries.forEach(metric -> otherRisk.addAndGet(metric.getRisk()));

    if (otherRisk.get() != thisRisk.get()) {
      return otherRisk.get() - thisRisk.get();
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

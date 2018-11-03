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

    return otherRisk.get() - thisRisk.get();
  }
}

package io.harness.cvng.analysis.entities;

import static io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums.MetricSum;
import static io.harness.cvng.analysis.entities.TimeSeriesCumulativeSums.TransactionMetricSums;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeSeriesCumulativeSumsTest extends CategoryTest {
  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testConvertMapToTransactionMetricSums() {
    List<TransactionMetricSums> transactionMetricSums =
        TimeSeriesCumulativeSums.convertMapToTransactionMetricSums(buildMetricSumsMap());

    assertThat(transactionMetricSums).isNotNull();
    assertThat(transactionMetricSums.size()).isEqualTo(3);
    transactionMetricSums.forEach(transactionMetricSum -> {
      assertThat(Arrays.asList("txn1", "txn2", "txn3").contains(transactionMetricSum.getTransactionName())).isTrue();
      List<MetricSum> metricSumsList = transactionMetricSum.getMetricSums();
      assertThat(metricSumsList.size()).isEqualTo(3);
      metricSumsList.forEach(metricSums -> {
        assertThat(Arrays.asList("metric1", "metric2", "metric3").contains(metricSums.getMetricName())).isTrue();
        assertThat(metricSums.getSum()).isEqualTo(0.9);
        assertThat(metricSums.getRisk()).isEqualTo(0.5);
      });
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testConvertMapToTransactionMetricSums_null() {
    List<TransactionMetricSums> transactionMetricSums =
        TimeSeriesCumulativeSums.convertMapToTransactionMetricSums(null);

    assertThat(transactionMetricSums).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testConvertToMap() {
    TimeSeriesCumulativeSums timeSeriesCumulativeSums = TimeSeriesCumulativeSums.builder()
                                                            .cvConfigId(generateUuid())
                                                            .transactionMetricSums(buildTransactionMetricSums())
                                                            .build();

    Map<String, Map<String, MetricSum>> txnMetricMap = timeSeriesCumulativeSums.convertToMap();

    assertThat(txnMetricMap.size()).isEqualTo(3);
    assertThat(txnMetricMap.containsKey("txn1")).isTrue();
    assertThat(txnMetricMap.containsKey("txn2")).isTrue();
    assertThat(txnMetricMap.containsKey("txn3")).isTrue();

    for (String txn : txnMetricMap.keySet()) {
      Map<String, MetricSum> metricSumsMap = txnMetricMap.get(txn);
      assertThat(metricSumsMap.size()).isEqualTo(3);
      assertThat(metricSumsMap.containsKey("metric1")).isTrue();
      assertThat(metricSumsMap.containsKey("metric2")).isTrue();
      assertThat(metricSumsMap.containsKey("metric3")).isTrue();
      MetricSum sums = metricSumsMap.get("metric1");
      assertThat(sums.getRisk()).isEqualTo(0.5);
      assertThat(sums.getSum()).isEqualTo(0.9);
    }
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testConvertToMap_nullSums() {
    TimeSeriesCumulativeSums timeSeriesCumulativeSums =
        TimeSeriesCumulativeSums.builder().cvConfigId(generateUuid()).transactionMetricSums(null).build();

    Map<String, Map<String, MetricSum>> txnMetricMap = timeSeriesCumulativeSums.convertToMap();
    assertThat(txnMetricMap).isNotNull();
    assertThat(txnMetricMap.size()).isEqualTo(0);
  }

  private List<TransactionMetricSums> buildTransactionMetricSums() {
    List<TransactionMetricSums> txnMetricSums = new ArrayList<>();
    List<String> transactions = Arrays.asList("txn1", "txn2", "txn3");
    List<String> metricList = Arrays.asList("metric1", "metric2", "metric3");

    transactions.forEach(txn -> {
      TransactionMetricSums transactionMetricSums =
          TransactionMetricSums.builder().transactionName(txn).metricSums(new ArrayList<>()).build();

      metricList.forEach(metric -> {
        MetricSum metricSums = MetricSum.builder().metricName(metric).risk(0.5).sum(0.9).build();
        transactionMetricSums.getMetricSums().add(metricSums);
      });
      txnMetricSums.add(transactionMetricSums);
    });
    return txnMetricSums;
  }

  private Map<String, Map<String, MetricSum>> buildMetricSumsMap() {
    Map<String, Map<String, MetricSum>> txnMetricSumMap = new HashMap<>();
    List<String> transactions = Arrays.asList("txn1", "txn2", "txn3");
    List<String> metricList = Arrays.asList("metric1", "metric2", "metric3");
    transactions.forEach(txn -> {
      txnMetricSumMap.put(txn, new HashMap<>());
      metricList.forEach(metric -> {
        Map<String, MetricSum> metricSumsMap = txnMetricSumMap.get(txn);
        metricSumsMap.put(metric, MetricSum.builder().risk(0.5).sum(0.9).build());
      });
    });
    return txnMetricSumMap;
  }
}
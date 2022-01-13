/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.metrics.MetricType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TimeSeriesRawDataTest extends CategoryTest {
  private String accountId = "accountId";
  private String stateExecutionId = "stateExecutionId";
  private String serviceId = "serviceId";
  private ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
  private String transaction1 = "transaction1";
  private String transaction2 = "transaction2";
  private String metric1 = "metric1";
  private String metric2 = "metric2";
  private String host1 = "host1";
  private String host2 = "host2";

  private MetricAnalysisRecord getMetricAnalysisRecord(int analysisMinute) {
    MetricAnalysisRecord record = new MetricAnalysisRecord();
    record.setStateExecutionId(stateExecutionId);
    record.setAnalysisMinute(analysisMinute);
    return record;
  }

  private void addTransaction(MetricAnalysisRecord record, String transactionName, String metricName, String host,
      List<Double> controlData, List<Double> testData) {
    record.decompress(false);

    if (record.getTransactions() == null) {
      record.setTransactions(new HashMap<>());
    }

    TimeSeriesMLTxnSummary summary;

    if (record.getTransactions().containsKey(transactionName)) {
      summary = record.getTransactions().get(transactionName);
    } else {
      summary = new TimeSeriesMLTxnSummary();
      summary.setTxn_name(transactionName);
      summary.setMetrics(new HashMap<>());
    }

    TimeSeriesMLMetricSummary metricSummary;

    if (summary.getMetrics().containsKey(metricName)) {
      metricSummary = summary.getMetrics().get(metricName);
    } else {
      metricSummary = new TimeSeriesMLMetricSummary();
      metricSummary.setMetric_name(metricName);
      metricSummary.setMetric_type(MetricType.THROUGHPUT.name());
      metricSummary.setResults(new HashMap<>());
    }

    TimeSeriesMLHostSummary hostSummary;

    if (metricSummary.getResults().containsKey(host)) {
      hostSummary = metricSummary.getResults().get(host);
    } else {
      hostSummary = TimeSeriesMLHostSummary.builder().build();
      hostSummary.setControl_data(controlData);
      hostSummary.setTest_data(testData);
    }

    metricSummary.getResults().put(host, hostSummary);

    summary.getMetrics().put(metricName, metricSummary);

    record.getTransactions().put(transactionName, summary);
    record.bundleAsJosnAndCompress();
  }

  private List<TimeSeriesRawData> getRawDataList(Map<String, Map<String, TimeSeriesRawData>> existingRawDataMap) {
    List<TimeSeriesRawData> rawDataList = new ArrayList<>();
    existingRawDataMap.values().forEach(metricMap -> rawDataList.addAll(metricMap.values()));
    return rawDataList;
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testRecordPopulation() {
    Map<String, Map<String, TimeSeriesRawData>> existingRawDataMap = new HashMap<>();

    MetricAnalysisRecord record = getMetricAnalysisRecord(0);
    addTransaction(record, transaction1, metric1, host1, Arrays.asList(0.1, 0.2), Arrays.asList(0.2, 0.33));
    addTransaction(record, transaction1, metric2, host1, Arrays.asList(0.1, 0.2), Arrays.asList(0.2, 0.33));
    addTransaction(record, transaction2, metric1, host1, Arrays.asList(0.1, 0.2), Arrays.asList(0.2, 0.33));
    addTransaction(record, transaction2, metric2, host2, Arrays.asList(0.1, 0.2), Arrays.asList(0.2, 0.33));

    TimeSeriesRawData.populateRawDataFromAnalysisRecords(
        record, accountId, executionStatus, existingRawDataMap, serviceId);

    assertThat(existingRawDataMap.keySet()).hasSize(2);
    assertThat(getRawDataList(existingRawDataMap)).hasSize(4);
  }
}

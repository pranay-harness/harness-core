/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.datacollection.entity.TimeSeriesRecord;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TimeSeriesDataStoreServiceTest extends CategoryTest {
  @Test
  @Owner(developers = OwnerRule.RAGHU)
  @Category(UnitTests.class)
  public void testConvertToCollectionRecords() {
    String accountId = generateUuid();
    String verificationTaskId = generateUuid();
    int numOfMins = 10;
    int numOfMetrics = 5;
    int numOfTxns = 15;
    Set<String> metrics = new HashSet<>();
    Set<String> txns = new HashSet<>();
    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    for (int i = 0; i < numOfMins; i++) {
      for (int j = 0; j < numOfMetrics; j++) {
        metrics.add("metric-" + j);
        for (int k = 0; k < numOfTxns; k++) {
          txns.add("txn-" + k);
          timeSeriesRecords.add(TimeSeriesRecord.builder()
                                    .txnName("txn-" + k)
                                    .metricName("metric-" + j)
                                    .metricValue(10.0)
                                    .timestamp(TimeUnit.MINUTES.toMillis(i))
                                    .build());
        }
      }
    }

    List<TimeSeriesDataCollectionRecord> dataCollectionRecords =
        new TimeSeriesDataStoreService().convertToCollectionRecords(accountId, verificationTaskId, timeSeriesRecords);
    System.out.println(dataCollectionRecords);
    assertThat(dataCollectionRecords.size()).isEqualTo(numOfMins);
    Collections.sort(dataCollectionRecords, Comparator.comparingLong(TimeSeriesDataCollectionRecord::getTimeStamp));
    for (int i = 0; i < numOfMins; i++) {
      TimeSeriesDataCollectionRecord dataCollectionRecord = dataCollectionRecords.get(i);
      assertThat(dataCollectionRecord.getTimeStamp()).isEqualTo(TimeUnit.MINUTES.toMillis(i));
      assertThat(dataCollectionRecord.getMetricValues().size()).isEqualTo(numOfMetrics);
      dataCollectionRecord.getMetricValues().forEach(recordMetricValue -> {
        assertThat(metrics).contains(recordMetricValue.getMetricName());
        assertThat(recordMetricValue.getTimeSeriesValues().size()).isEqualTo(numOfTxns);
        recordMetricValue.getTimeSeriesValues().forEach(groupValue -> {
          assertThat(txns).contains(groupValue.getGroupName());
          assertThat(groupValue.getValue()).isEqualTo(10.0, offset(0.001));
        });
      });
    }
  }
}

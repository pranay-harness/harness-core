/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.newrelic;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NewRelicMetricDataRecordTest extends WingsBaseTest {
  private Random random = new Random();

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testConvertErrorsToPercentage_whenEmpty() {
    Map<String, Double> values = new HashMap<>();
    values.put(generateUuid(), random.nextDouble());
    NewRelicMetricDataRecord metricDataRecord =
        NewRelicMetricDataRecord.builder().values(new HashMap<>(values)).build();
    metricDataRecord.convertErrorsToPercentage(Collections.emptyMap());
    assertThat(metricDataRecord.getValues()).isEqualTo(values);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testConvertErrorsToPercentage_whenThroughputNoErrors() {
    Map<String, Double> values = new HashMap<>();
    values.put("throughput", random.nextDouble());
    NewRelicMetricDataRecord metricDataRecord =
        NewRelicMetricDataRecord.builder().name("txn1").values(new HashMap<>(values)).build();

    TreeBasedTable<String, String, List<String>> throughputToErrorsMap = TreeBasedTable.create();
    throughputToErrorsMap.put("txn1", "throughput", Lists.newArrayList("error2", "error3"));
    metricDataRecord.convertErrorsToPercentage(throughputToErrorsMap.row(metricDataRecord.getName()));
    assertThat(metricDataRecord.getValues()).isEqualTo(values);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testConvertErrorsToPercentage_whenErrorsNoThroughput() {
    Map<String, Double> values = new HashMap<>();
    values.put("error1", random.nextDouble());
    NewRelicMetricDataRecord metricDataRecord =
        NewRelicMetricDataRecord.builder().name("txn1").values(new HashMap<>(values)).build();

    TreeBasedTable<String, String, List<String>> throughputToErrorsMap = TreeBasedTable.create();
    throughputToErrorsMap.put("txn1", "throughput", Lists.newArrayList("error1", "error2"));
    metricDataRecord.convertErrorsToPercentage(throughputToErrorsMap.row(metricDataRecord.getName()));
    assertThat(metricDataRecord.getValues()).isEqualTo(values);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testConvertErrorsToPercentage_whenZeroThroughput() {
    Map<String, Double> values = new HashMap<>();
    values.put("throughput", 0D);
    values.put("error1", random.nextDouble());
    values.put("error2", random.nextDouble());
    NewRelicMetricDataRecord metricDataRecord =
        NewRelicMetricDataRecord.builder().name("txn1").values(new HashMap<>(values)).build();

    TreeBasedTable<String, String, List<String>> throughputToErrorsMap = TreeBasedTable.create();
    throughputToErrorsMap.put("txn1", "throughput", Lists.newArrayList("error1", "error2"));
    metricDataRecord.convertErrorsToPercentage(throughputToErrorsMap.row(metricDataRecord.getName()));
    assertThat(metricDataRecord.getValues()).isEqualTo(values);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testConvertErrorsToPercentage_whenThroughputAndError() {
    Map<String, Double> values = new HashMap<>();
    values.put("throughput", random.nextDouble());
    values.put("error1", random.nextDouble());
    values.put("error2", random.nextDouble());
    values.put("error3", random.nextDouble());
    NewRelicMetricDataRecord metricDataRecord =
        NewRelicMetricDataRecord.builder().name("txn1").values(new HashMap<>(values)).build();

    TreeBasedTable<String, String, List<String>> throughputToErrorsMap = TreeBasedTable.create();
    throughputToErrorsMap.put("txn1", "throughput", Lists.newArrayList("error1", "error2"));
    metricDataRecord.convertErrorsToPercentage(throughputToErrorsMap.row(metricDataRecord.getName()));

    final Double throughput = values.get("throughput");
    assertThat(metricDataRecord.getValues().get("throughput")).isCloseTo(throughput, offset(0.01));
    assertThat(metricDataRecord.getValues().get("error3")).isCloseTo(values.get("error3"), offset(0.01));
    double error1 = values.get("error1") * 100 / throughput;
    assertThat(metricDataRecord.getValues().get("error1")).isCloseTo(error1, offset(0.01));

    double error2 = values.get("error2") * 100 / throughput;
    assertThat(metricDataRecord.getValues().get("error2")).isCloseTo(error2, offset(0.01));
  }
}

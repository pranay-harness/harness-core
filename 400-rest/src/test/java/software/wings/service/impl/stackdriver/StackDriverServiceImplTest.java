/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.stackdriver;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.VerificationOperationException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.metrics.MetricType;
import software.wings.service.intfc.stackdriver.StackDriverService;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class StackDriverServiceImplTest extends WingsBaseTest {
  @Inject private StackDriverService stackDriverService;

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateMetricDefinitions_emptyMetrics() {
    assertThatThrownBy(() -> stackDriverService.validateMetricDefinitions(new ArrayList<>()))
        .isInstanceOf(VerificationOperationException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateMetricDefinitions_duplicateMetricsWithDifferentMetricTypes() {
    List<StackDriverMetricDefinition> definitions = Arrays.asList(StackDriverMetricDefinition.builder()
                                                                      .metricName("test")
                                                                      .metricType(MetricType.INFRA.name())
                                                                      .txnName("txn")
                                                                      .filter("{}")
                                                                      .build(),
        StackDriverMetricDefinition.builder()
            .metricName("test")
            .txnName("txn")
            .metricType(MetricType.THROUGHPUT.name())
            .filter("{}")
            .build());
    assertThatThrownBy(() -> stackDriverService.validateMetricDefinitions(definitions))
        .isInstanceOf(VerificationOperationException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateMetricDefinitions_noThroughputMetricsWithErrorOrResponseTimeMetrics() {
    List<StackDriverMetricDefinition> definitions = Arrays.asList(StackDriverMetricDefinition.builder()
                                                                      .metricName("test")
                                                                      .metricType(MetricType.ERROR.name())
                                                                      .txnName("txn")
                                                                      .filter("{}")
                                                                      .build(),
        StackDriverMetricDefinition.builder()
            .metricName("response")
            .txnName("txn")
            .metricType(MetricType.RESP_TIME.name())
            .filter("{}")
            .build());
    assertThatThrownBy(() -> stackDriverService.validateMetricDefinitions(definitions))
        .isInstanceOf(VerificationOperationException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateMetricDefinitions_moreThanOneThroughputMetricsWithErrorOrResponseTimeMetrics() {
    List<StackDriverMetricDefinition> definitions = Arrays.asList(StackDriverMetricDefinition.builder()
                                                                      .metricName("test")
                                                                      .metricType(MetricType.ERROR.name())
                                                                      .txnName("txn")
                                                                      .filter("{}")
                                                                      .build(),
        StackDriverMetricDefinition.builder()
            .metricName("response")
            .txnName("txn")
            .metricType(MetricType.RESP_TIME.name())
            .filter("{}")
            .build(),
        StackDriverMetricDefinition.builder()
            .metricName("calls_per_minute")
            .txnName("txn")
            .metricType(MetricType.THROUGHPUT.name())
            .filter("{}")
            .build(),
        StackDriverMetricDefinition.builder()
            .metricName("traffic")
            .txnName("txn")
            .metricType(MetricType.THROUGHPUT.name())
            .filter("{}")
            .build());
    assertThatThrownBy(() -> stackDriverService.validateMetricDefinitions(definitions))
        .isInstanceOf(VerificationOperationException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateMetricDefinitions_onlyThrouputMetrics() {
    List<StackDriverMetricDefinition> definitions = Arrays.asList(StackDriverMetricDefinition.builder()
                                                                      .metricName("calls_per_minute")
                                                                      .txnName("txn")
                                                                      .metricType(MetricType.THROUGHPUT.name())
                                                                      .filter("{}")
                                                                      .build(),
        StackDriverMetricDefinition.builder()
            .metricName("traffic")
            .txnName("txn")
            .metricType(MetricType.THROUGHPUT.name())
            .filter("{}")
            .build());
    assertThatThrownBy(() -> stackDriverService.validateMetricDefinitions(definitions))
        .isInstanceOf(VerificationOperationException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testValidateMetricDefinitions_validationSuccess() {
    List<StackDriverMetricDefinition> definitions = Arrays.asList(StackDriverMetricDefinition.builder()
                                                                      .metricName("calls_per_minute")
                                                                      .txnName("txn")
                                                                      .metricType(MetricType.THROUGHPUT.name())
                                                                      .filter("{}")
                                                                      .build(),
        StackDriverMetricDefinition.builder()
            .metricName("cpu")
            .txnName("txn")
            .metricType(MetricType.INFRA.name())
            .filter("{}")
            .build());
    assertThatCode(() -> stackDriverService.validateMetricDefinitions(definitions)).doesNotThrowAnyException();
  }
}

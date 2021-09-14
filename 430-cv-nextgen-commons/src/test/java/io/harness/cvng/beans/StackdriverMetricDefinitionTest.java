/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.beans;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.stackdriver.StackDriverMetricDefinition;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StackdriverMetricDefinitionTest extends CategoryTest {
  ClassLoader classLoader = getClass().getClassLoader();
  @Before
  public void setup() throws IOException {}

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExtractFromJson() throws Exception {
    String textLoad = Resources.toString(
        Objects.requireNonNull(classLoader.getResource("metricDefinition.json")), StandardCharsets.UTF_8);

    StackDriverMetricDefinition metricDefinition = StackDriverMetricDefinition.extractFromJson(textLoad);

    assertThat(metricDefinition).isNotNull();
    assertThat(metricDefinition.getFilter())
        .isEqualTo("metric.type=\"kubernetes.io/container/restart_count\" resource.type=\"k8s_container\"");
    assertThat(metricDefinition.getAggregation()).isNotNull();
    assertThat(metricDefinition.getAggregation().getPerSeriesAligner()).isEqualTo("ALIGN_RATE");
    assertThat(metricDefinition.getAggregation().getCrossSeriesReducer()).isEqualTo("REDUCE_SUM");
    assertThat(metricDefinition.getAggregation().getAlignmentPeriod()).isEqualTo("60s");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testExtractFromJson_manualFormat() throws Exception {
    String textLoad =
        Resources.toString(Objects.requireNonNull(classLoader.getResource("stackdriver-metricDefinition-manual.json")),
            StandardCharsets.UTF_8);

    StackDriverMetricDefinition metricDefinition = StackDriverMetricDefinition.extractFromJson(textLoad);

    assertThat(metricDefinition).isNotNull();
    assertThat(metricDefinition.getFilter())
        .isEqualTo("metric.type=\"kubernetes.io/container/memory/used_bytes\" resource.type=\"k8s_container\"");
    assertThat(metricDefinition.getAggregation()).isNotNull();
    assertThat(metricDefinition.getAggregation().getPerSeriesAligner()).isEqualTo("ALIGN_MEAN");
    assertThat(metricDefinition.getAggregation().getCrossSeriesReducer()).isEqualTo("REDUCE_MEAN");
    assertThat(metricDefinition.getAggregation().getAlignmentPeriod()).isEqualTo("60s");
  }
}

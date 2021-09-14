/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.utils.monitoredService;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.StackdriverDefinition;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.StackdriverMetricHealthSourceSpec;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StackdriverMetricHealthSourceSpecTransformerTest extends CvNextGenTestBase {
  String connectorIdentifier;
  String identifier;
  String monitoringSourceName;
  BuilderFactory builderFactory;

  @Inject StackdriverMetricHealthSourceSpecTransformer stackdriverMetricHealthSourceSpecTransformer;
  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    connectorIdentifier = "connectorId";
    identifier = "healthSourceIdentifier";
    monitoringSourceName = "Prometheus";
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig() throws Exception {
    StackdriverMetricHealthSourceSpec spec =
        stackdriverMetricHealthSourceSpecTransformer.transform(Arrays.asList(createCVConfig()));
    String metricDef = Resources.toString(
        StackdriverMetricHealthSourceSpecTransformerTest.class.getResource("/stackdriver/metric-definition.json"),
        Charsets.UTF_8);

    assertThat(spec).isNotNull();
    assertThat(spec.getConnectorRef()).isEqualTo(connectorIdentifier);
    assertThat(spec.getMetricDefinitions().size()).isEqualTo(1);
    StackdriverDefinition metricDefinition = spec.getMetricDefinitions().get(0);
    assertThat(metricDefinition.getMetricName()).isEqualTo("metricName1");
    assertThat(metricDefinition.getJsonMetricDefinition()).isEqualTo(JsonUtils.asObject(metricDef, Object.class));
    assertThat(metricDefinition.getRiskProfile().getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
  }

  private StackdriverCVConfig createCVConfig() throws IOException {
    StackdriverCVConfig cvConfig = builderFactory.stackdriverMetricCVConfigBuilder().build();
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    String metricDef = Resources.toString(
        StackdriverMetricHealthSourceSpecTransformerTest.class.getResource("/stackdriver/metric-definition.json"),
        Charsets.UTF_8);

    StackdriverCVConfig.MetricInfo metricInfo =
        StackdriverCVConfig.MetricInfo.builder().metricName("metricName1").jsonMetricDefinition(metricDef).build();
    cvConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build());
    cvConfig.setMetricInfoList(Arrays.asList(metricInfo));
    return cvConfig;
  }
}

package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.cvng.api.CVConfigTransformerTestBase;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.StackdriverDSConfig;
import io.harness.cvng.core.beans.StackdriverDefinition;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.api.StackdriverServiceImplTest;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StackdriverCVConfigTransformerTest extends CVConfigTransformerTestBase {
  @Inject private StackdriverCVConfigTransformer stackdriverCVConfigTransformer;
  @Before
  public void setup() {
    super.setUp();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void transformToDSConfig_precondition() {
    assertThatThrownBy(() -> stackdriverCVConfigTransformer.transform(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("List of cvConfigs can not empty");
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void transformToDSConfig_with1CVConfig() throws Exception {
    StackdriverCVConfig stackdriverCVConfig = StackdriverCVConfig.builder().dashboardName("dashboard").build();

    Set<MetricPack.MetricDefinition> metricDefs = new HashSet<>();
    metricDefs.add(
        MetricPack.MetricDefinition.builder()
            .name("metricName1")
            .type(TimeSeriesMetricType.RESP_TIME)
            .thresholds(Arrays.asList(TimeSeriesThreshold.builder()
                                          .metricName("metricName1")
                                          .metricType(TimeSeriesMetricType.RESP_TIME)
                                          .criteria(TimeSeriesThresholdCriteria.builder()
                                                        .criteria(" > 0.2")
                                                        .thresholdType(TimeSeriesThresholdType.ACT_WHEN_HIGHER)
                                                        .build())
                                          .build()

                    ))
            .build());

    MetricPack metricPack = MetricPack.builder()
                                .category(CVMonitoringCategory.PERFORMANCE)
                                .metrics(metricDefs)
                                .dataCollectionDsl("metric-pack-dsl")
                                .build();

    String metricDef = Resources.toString(
        StackdriverServiceImplTest.class.getResource("/stackdriver/metric-definition.json"), Charsets.UTF_8);

    StackdriverCVConfig.MetricInfo metricInfo =
        StackdriverCVConfig.MetricInfo.builder().metricName("metricName1").jsonMetricDefinition(metricDef).build();
    stackdriverCVConfig.setMetricPack(metricPack);
    stackdriverCVConfig.setMetricInfoList(Arrays.asList(metricInfo));
    fillCommonFields(stackdriverCVConfig);

    StackdriverDSConfig dsConfig =
        stackdriverCVConfigTransformer.transformToDSConfig(Arrays.asList(stackdriverCVConfig));

    assertThat(dsConfig).isNotNull();
    assertThat(dsConfig.getMetricDefinitions().size()).isEqualTo(1);
    assertThat(dsConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);

    StackdriverDefinition metricDefinition = dsConfig.getMetricDefinitions().iterator().next();

    assertThat(metricDefinition.getRiskProfile().getCategory().name())
        .isEqualTo(CVMonitoringCategory.PERFORMANCE.name());
    assertThat(metricDefinition.getRiskProfile().getThresholdTypes().size()).isEqualTo(1);
    assertThat(metricDefinition.getRiskProfile().getThresholdTypes().get(0).name())
        .isEqualTo(TimeSeriesThresholdType.ACT_WHEN_HIGHER.name());
    assertThat(metricDefinition.getMetricName()).isEqualTo("metricName1");
    assertThat(metricDefinition.getJsonMetricDefinition()).isEqualTo(metricDef);
  }
}

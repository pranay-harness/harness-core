package io.harness.cvng.core.utils.monitoredService;

import com.google.inject.Inject;
import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.DatadogMetricHealthDefinition;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.DatadogMetricHealthSourceSpec;
import io.harness.cvng.core.entities.DatadogMetricCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.rule.Owner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.harness.rule.OwnerRule.PAVIC;
import static org.assertj.core.api.Assertions.assertThat;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DatadogMetricHealthSourceSpecTransformerTest extends CvNextGenTestBase {
  private static final int METRIC_DEFINITIONS_COUNT = 5;
  private static final String MOCKED_METRIC_NAME = "testMetricName";
  private static final String MOCKED_METRIC_QUERY = "system.user.cpu{*}";
  private static final String CONNECTOR_IDENTIFIER = "connectorId";

  private BuilderFactory builderFactory;
  @Inject DatadogMetricHealthSourceSpecTransformer classUnderTest;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = PAVIC)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig() {
    DatadogMetricHealthSourceSpec datadogMetricHealthSourceSpec =
        classUnderTest.transform(Collections.singletonList(createCVConfig()));

    assertThat(datadogMetricHealthSourceSpec).isNotNull();
    assertThat(datadogMetricHealthSourceSpec.getConnectorRef()).isEqualTo(CONNECTOR_IDENTIFIER);
    assertThat(datadogMetricHealthSourceSpec.getMetricDefinitions().size()).isEqualTo(METRIC_DEFINITIONS_COUNT);
    DatadogMetricHealthDefinition metricDefinition = datadogMetricHealthSourceSpec.getMetricDefinitions().get(0);
    assertThat(metricDefinition.getMetricName()).isEqualTo(MOCKED_METRIC_NAME);
    assertThat(metricDefinition.getQuery()).isEqualTo(MOCKED_METRIC_QUERY);
    assertThat(metricDefinition.getRiskProfile().getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
    assertThat(metricDefinition.getRiskProfile().getMetricType()).isEqualTo(TimeSeriesMetricType.RESP_TIME);
  }

  private DatadogMetricCVConfig createCVConfig() {
    DatadogMetricCVConfig cvConfig = builderFactory.datadogMetricCVConfigBuilder().build();
    cvConfig.setConnectorIdentifier(CONNECTOR_IDENTIFIER);
    cvConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.ERRORS).build());

    cvConfig.setMetricInfoList( IntStream.range(0, METRIC_DEFINITIONS_COUNT)
            .mapToObj(index ->
                    DatadogMetricCVConfig.MetricInfo.builder()
                            .metricName(MOCKED_METRIC_NAME)
                            .metricType(TimeSeriesMetricType.RESP_TIME)
                            .query(MOCKED_METRIC_QUERY)
                            .build()).collect(Collectors.toList()));
    return cvConfig;
  }
}

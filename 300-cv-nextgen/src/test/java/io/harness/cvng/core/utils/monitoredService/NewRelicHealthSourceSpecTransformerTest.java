package io.harness.cvng.core.utils.monitoredService;

import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.NewRelicHealthSourceSpec;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.NewRelicCVConfig;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewRelicHealthSourceSpecTransformerTest extends CvNextGenTestBase {
  MetricPack metricPack;
  String applicationName;
  String connectorIdentifier;
  String productName;
  String identifier;
  String monitoringSourceName;
  String applicationId;
  BuilderFactory builderFactory;

  @Inject NewRelicHealthSourceSpecTransformer newRelicHealthSourceSpecTransformer;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    metricPack = MetricPack.builder().identifier("Performance").category(CVMonitoringCategory.PERFORMANCE).build();
    applicationName = "appName";
    connectorIdentifier = "connectorId";
    productName = "apm";
    identifier = "healthSourceIdentifier";
    monitoringSourceName = "AppDynamics";
    applicationId = "1234";
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig() {
    NewRelicCVConfig newRelicCVConfig = createCVConfig();
    NewRelicHealthSourceSpec newRelicHealthSourceSpec =
        newRelicHealthSourceSpecTransformer.transform(Arrays.asList(newRelicCVConfig));

    assertThat(newRelicHealthSourceSpec.getApplicationName()).isEqualTo(applicationName);
    assertThat(newRelicHealthSourceSpec.getConnectorRef()).isEqualTo(connectorIdentifier);
    assertThat(newRelicHealthSourceSpec.getApplicationId()).isEqualTo(applicationId);
    assertThat(newRelicHealthSourceSpec.getFeature()).isEqualTo(productName);
    assertThat(newRelicHealthSourceSpec.getMetricPacks().size()).isEqualTo(1);
  }

  private NewRelicCVConfig createCVConfig() {
    return (NewRelicCVConfig) builderFactory.newRelicCVConfigBuilder()
        .applicationId(Long.valueOf(applicationId))
        .applicationName(applicationName)
        .metricPack(metricPack)
        .connectorIdentifier(connectorIdentifier)
        .productName(productName)
        .identifier(identifier)
        .monitoringSourceName(monitoringSourceName)
        .build();
  }
}

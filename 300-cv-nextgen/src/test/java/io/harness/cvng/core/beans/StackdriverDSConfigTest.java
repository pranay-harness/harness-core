package io.harness.cvng.core.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.DSConfig.CVConfigUpdateResult;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.MetricPack.MetricDefinition;
import io.harness.cvng.core.entities.StackdriverCVConfig;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class StackdriverDSConfigTest extends DSConfigTestBase {
  StackdriverDSConfig stackdriverDSConfig;
  private List<MetricPack> metricPacks;
  String serviceIdentifier;

  @Before
  public void setup() {
    stackdriverDSConfig = new StackdriverDSConfig();
    fillCommonFields(stackdriverDSConfig);
    metricPacks = Arrays.asList(createMetricPack(
        Arrays.asList(TimeSeriesMetricType.THROUGHPUT, TimeSeriesMetricType.ERROR, TimeSeriesMetricType.RESP_TIME)));
    stackdriverDSConfig.setMetricPacks(Sets.newHashSet(metricPacks));
    serviceIdentifier = generateUuid();
    stackdriverDSConfig.setServiceIdentifier(serviceIdentifier);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenNoConfigExists() {
    Set<StackdriverDefinition> definitions = new HashSet<>();
    definitions.add(StackdriverDefinition.builder()
                        .jsonMetricDefinition("{\"sampleJson\":[]}")
                        .metricName("metricName")
                        .dashboardName("dashboardName")
                        .metricTags(Arrays.asList("tag1"))
                        .riskProfile(StackdriverDefinition.RiskProfile.builder()
                                         .metricType(TimeSeriesMetricType.RESP_TIME)
                                         .category(CVMonitoringCategory.PERFORMANCE)
                                         .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                         .build())
                        .build());
    stackdriverDSConfig.setMetricDefinitions(definitions);

    CVConfigUpdateResult cvConfigUpdateResult = stackdriverDSConfig.getCVConfigUpdateResult(Collections.emptyList());

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);

    List<StackdriverCVConfig> cvConfigs = (List<StackdriverCVConfig>) (List<?>) cvConfigUpdateResult.getAdded();
    cvConfigs.forEach(cvConfig -> {
      assertThat(cvConfig.getDashboardName()).isEqualTo("dashboardName");
      assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
      assertThat(cvConfig.getCategory().name()).isEqualTo(CVMonitoringCategory.PERFORMANCE.name());
    });
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenDeleted() {
    Set<StackdriverDefinition> definitions = new HashSet<>();
    definitions.add(StackdriverDefinition.builder()
                        .jsonMetricDefinition("{\"sampleJson\":[]}")
                        .metricName("metricName")
                        .dashboardName("dashboardName")
                        .metricTags(Arrays.asList("tag1"))
                        .riskProfile(StackdriverDefinition.RiskProfile.builder()
                                         .metricType(TimeSeriesMetricType.RESP_TIME)
                                         .category(CVMonitoringCategory.PERFORMANCE)
                                         .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                         .build())
                        .build());
    stackdriverDSConfig.setMetricDefinitions(definitions);

    StackdriverCVConfig existingCVConfig = StackdriverCVConfig.builder().dashboardName("dashboard2").build();
    existingCVConfig.setEnvIdentifier(envIdentifier);
    existingCVConfig.setServiceIdentifier(serviceIdentifier);
    existingCVConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build());

    CVConfigUpdateResult cvConfigUpdateResult =
        stackdriverDSConfig.getCVConfigUpdateResult(Arrays.asList(existingCVConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isNotEmpty();

    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);
    assertThat(cvConfigUpdateResult.getDeleted().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenUpdated() {
    Set<StackdriverDefinition> definitions = new HashSet<>();
    definitions.add(StackdriverDefinition.builder()
                        .jsonMetricDefinition("{\"sampleJson\":[]}")
                        .metricName("metricName")
                        .dashboardName("dashboardName")
                        .metricTags(Arrays.asList("tag1"))
                        .riskProfile(StackdriverDefinition.RiskProfile.builder()
                                         .metricType(TimeSeriesMetricType.RESP_TIME)
                                         .category(CVMonitoringCategory.PERFORMANCE)
                                         .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                         .build())
                        .build());
    stackdriverDSConfig.setMetricDefinitions(definitions);

    StackdriverCVConfig existingCVConfig = StackdriverCVConfig.builder().dashboardName("dashboardName").build();
    existingCVConfig.setEnvIdentifier(envIdentifier);
    existingCVConfig.setServiceIdentifier(serviceIdentifier);
    existingCVConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build());

    CVConfigUpdateResult cvConfigUpdateResult =
        stackdriverDSConfig.getCVConfigUpdateResult(Arrays.asList(existingCVConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getUpdated().size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenAddedWithExising() {
    Set<StackdriverDefinition> definitions = new HashSet<>();
    definitions.add(StackdriverDefinition.builder()
                        .jsonMetricDefinition("{\"sampleJson\":[]}")
                        .metricName("metricName")
                        .dashboardName("dashboardName")
                        .metricTags(Arrays.asList("tag1"))
                        .riskProfile(StackdriverDefinition.RiskProfile.builder()
                                         .metricType(TimeSeriesMetricType.RESP_TIME)
                                         .category(CVMonitoringCategory.PERFORMANCE)
                                         .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                         .build())
                        .build());
    definitions.add(StackdriverDefinition.builder()
                        .jsonMetricDefinition("{\"sampleJson\":[]}")
                        .metricName("metricName")
                        .dashboardName("dashboardName2")
                        .metricTags(Arrays.asList("tag2"))
                        .riskProfile(StackdriverDefinition.RiskProfile.builder()
                                         .metricType(TimeSeriesMetricType.RESP_TIME)
                                         .category(CVMonitoringCategory.PERFORMANCE)
                                         .thresholdTypes(Arrays.asList(TimeSeriesThresholdType.ACT_WHEN_HIGHER))
                                         .build())
                        .build());
    stackdriverDSConfig.setMetricDefinitions(definitions);

    StackdriverCVConfig existingCVConfig = StackdriverCVConfig.builder().dashboardName("dashboardName2").build();
    existingCVConfig.setEnvIdentifier(envIdentifier);
    existingCVConfig.setServiceIdentifier(serviceIdentifier);
    existingCVConfig.setMetricPack(MetricPack.builder().category(CVMonitoringCategory.PERFORMANCE).build());

    CVConfigUpdateResult cvConfigUpdateResult =
        stackdriverDSConfig.getCVConfigUpdateResult(Arrays.asList(existingCVConfig));

    assertThat(cvConfigUpdateResult).isNotNull();
    assertThat(cvConfigUpdateResult.getAdded()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getUpdated()).isNotEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();

    assertThat(cvConfigUpdateResult.getUpdated().size()).isEqualTo(1);
    assertThat(cvConfigUpdateResult.getAdded().size()).isEqualTo(1);
  }

  private MetricPack createMetricPack(List<TimeSeriesMetricType> metricTypes) {
    Set<MetricDefinition> metricPacks = new HashSet<>();
    metricTypes.forEach(type -> { metricPacks.add(MetricDefinition.builder().name(type.name()).type(type).build()); });
    return MetricPack.builder()
        .accountId(accountId)
        .identifier("metric-pack-" + metricTypes.size())
        .category(CVMonitoringCategory.PERFORMANCE)
        .metrics(metricPacks)
        .build();
  }
}

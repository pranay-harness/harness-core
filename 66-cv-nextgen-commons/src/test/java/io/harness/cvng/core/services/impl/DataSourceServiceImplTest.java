package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.core.services.api.DataSourceService;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.models.DataSourceType;
import io.harness.rule.Owner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DataSourceServiceImplTest extends CVNextGenBaseTest {
  @Inject private DataSourceService dataSourceService;
  private String accountId;
  private String projectId;

  @Before
  public void setup() {
    accountId = generateUuid();
    projectId = generateUuid();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testMetricPackFilesAdded() {
    final URL metricPackUrl = DataSourceService.class.getResource("/metric-packs/appdynamics");
    final Collection<File> metricPackYamls = FileUtils.listFiles(new File(metricPackUrl.getFile()), null, false);
    assertThat(metricPackYamls.size()).isEqualTo(DataSourceServiceImpl.appdynamicsMetricPackFiles.size());
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetMetricPacksMap() {
    final Map<String, MetricPack> metricPacks =
        dataSourceService.getMetricPackMap(accountId, projectId, DataSourceType.APP_DYNAMICS);
    assertThat(metricPacks.size()).isGreaterThan(0);
    metricPacks.forEach((metricPackName, metricPack) -> {
      assertThat(metricPack.getName()).isNotEmpty();
      assertThat(metricPack.getName()).isEqualTo(metricPackName);
      assertThat(metricPack.getMetrics().size()).isGreaterThan(0);
      metricPack.getMetrics().forEach(metricDefinition -> {
        assertThat(metricDefinition.getName()).isNotEmpty();
        assertThat(metricDefinition.getPath()).isNotEmpty();
      });
    });
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetMetricPacks() {
    final Collection<MetricPack> metricPacks =
        dataSourceService.getMetricPacks(accountId, projectId, DataSourceType.APP_DYNAMICS, false);
    assertThat(metricPacks.size()).isGreaterThan(0);
    metricPacks.forEach(metricPack -> {
      assertThat(metricPack.getName()).isNotEmpty();
      assertThat(metricPack.getMetrics().size()).isGreaterThan(0);
      metricPack.getMetrics().forEach(metricDefinition -> {
        assertThat(metricDefinition.getName()).isNotEmpty();
        assertThat(metricDefinition.getPath()).isNotEmpty();
      });
    });
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetMetricPacks_whenExcludeDetails() {
    final Collection<MetricPack> metricPacks =
        dataSourceService.getMetricPacks(accountId, projectId, DataSourceType.APP_DYNAMICS, true);
    assertThat(metricPacks.size()).isGreaterThan(0);
    metricPacks.forEach(metricPack -> {
      assertThat(metricPack.getName()).isNotEmpty();
      assertThat(metricPack.getMetrics()).isEmpty();
    });
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testSaveMetricPacks() {
    Collection<MetricPack> metricPacks =
        dataSourceService.getMetricPacks(accountId, projectId, DataSourceType.APP_DYNAMICS, false);
    List<MetricPack> performancePacks =
        metricPacks.stream()
            .filter(metricPack -> metricPack.getName().equals("Performance and Availability"))
            .collect(Collectors.toList());
    assertThat(performancePacks.size()).isEqualTo(1);
    MetricPack performancePack = performancePacks.get(0);

    int performancePackSize = performancePack.getMetrics().size();
    performancePack.getMetrics().forEach(metric -> metric.setIncluded(true));
    assertThat(
        performancePack.getMetrics().remove(MetricPack.MetricDefinition.builder().name("Number of Slow Calls").build()))
        .isTrue();

    final boolean saved = dataSourceService.saveMetricPacks(
        accountId, projectId, DataSourceType.APP_DYNAMICS, Lists.newArrayList(performancePack));
    assertThat(saved).isTrue();

    metricPacks = dataSourceService.getMetricPacks(accountId, projectId, DataSourceType.APP_DYNAMICS, false);
    assertThat(metricPacks.size()).isGreaterThan(1);

    performancePacks = metricPacks.stream()
                           .filter(metricPack -> metricPack.getName().equals("Performance and Availability"))
                           .collect(Collectors.toList());
    assertThat(performancePacks.size()).isEqualTo(1);
    performancePack = performancePacks.get(0);

    assertThat(performancePack.getMetrics().size()).isEqualTo(performancePackSize);
    assertThat(performancePack.getMetrics())
        .contains(MetricPack.MetricDefinition.builder().name("Number of Slow Calls").build());
    performancePack.getMetrics().forEach(metricDefinition -> {
      if (metricDefinition.getName().equals("Number of Slow Calls")) {
        assertThat(metricDefinition.isIncluded()).isFalse();
      } else {
        assertThat(metricDefinition.isIncluded()).isTrue();
      }
    });
  }
}
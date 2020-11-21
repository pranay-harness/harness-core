package io.harness.cvng.core.beans;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.beans.DSConfig.CVConfigUpdateResult;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppDynamicsDSConfigTest extends DSConfigTestBase {
  private AppDynamicsDSConfig appDynamicsDSConfig;
  private List<MetricPack> metricPacks;
  private String appdApplicationName;
  @Before
  public void setup() {
    appDynamicsDSConfig = new AppDynamicsDSConfig();
    fillCommonFields(appDynamicsDSConfig);
    metricPacks = IntStream.range(0, 5).mapToObj(i -> createMetricPack(i)).collect(Collectors.toList());
    appdApplicationName = "appd application name";
    appDynamicsDSConfig.setIdentifier(appdApplicationName);
    appDynamicsDSConfig.setConnectorIdentifier(connectorIdentifier);
    appDynamicsDSConfig.setApplicationName(appdApplicationName);
    appDynamicsDSConfig.setProductName("Application monitoring");
    appDynamicsDSConfig.setEnvIdentifier("harnessProd");
    appDynamicsDSConfig.setAccountId(accountId);
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_whenNoConfigExist() {
    appDynamicsDSConfig.setMetricPacks(Sets.newHashSet(metricPacks.get(0)));
    appDynamicsDSConfig.setServiceMappings(Sets.newHashSet(
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service1").tierName("tier1").build()));
    CVConfigUpdateResult cvConfigUpdateResult = appDynamicsDSConfig.getCVConfigUpdateResult(Collections.emptyList());
    assertThat(cvConfigUpdateResult.getUpdated()).isEmpty();
    assertThat(cvConfigUpdateResult.getDeleted()).isEmpty();
    List<CVConfig> added = cvConfigUpdateResult.getAdded();
    List<AppDynamicsCVConfig> appDynamicsCVConfigs = (List<AppDynamicsCVConfig>) (List<?>) added;
    assertThat(appDynamicsCVConfigs).hasSize(1);
    appDynamicsCVConfigs.forEach(appDynamicsCVConfig -> {
      assertCommon(appDynamicsCVConfig, appDynamicsDSConfig);
      assertThat(appDynamicsCVConfig.getMetricPack().getIdentifier()).isEqualTo(metricPacks.get(0).getIdentifier());
    });
    assertThat(appDynamicsCVConfigs.get(0).getServiceIdentifier()).isEqualTo("service1");
    assertThat(appDynamicsCVConfigs.get(0).getTierName()).isEqualTo("tier1");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_checkDeleted() {
    appDynamicsDSConfig.setMetricPacks(Sets.newHashSet(metricPacks.get(0), metricPacks.get(1)));
    appDynamicsDSConfig.setServiceMappings(Sets.newHashSet(
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service1").tierName("tier1").build(),
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service2").tierName("tier2").build()));
    List<CVConfig> cvConfigs = new ArrayList<>();
    cvConfigs.add(create("service1", metricPacks.get(3), "tier1"));
    CVConfigUpdateResult result = appDynamicsDSConfig.getCVConfigUpdateResult(cvConfigs);
    assertThat(result.getDeleted()).hasSize(1);
    AppDynamicsCVConfig appDynamicsCVConfig = (AppDynamicsCVConfig) result.getDeleted().get(0);
    assertThat(appDynamicsCVConfig.getMetricPack()).isEqualTo(metricPacks.get(3));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_checkAdded() {
    appDynamicsDSConfig.setMetricPacks(Sets.newHashSet(metricPacks.get(0)));
    appDynamicsDSConfig.setServiceMappings(Sets.newHashSet(
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service1").tierName("tier1").build(),
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service2").tierName("tier2").build()));
    List<CVConfig> cvConfigs = new ArrayList<>();
    cvConfigs.add(create("service1", metricPacks.get(0), "tier1"));
    CVConfigUpdateResult result = appDynamicsDSConfig.getCVConfigUpdateResult(cvConfigs);
    assertThat(result.getAdded()).hasSize(1);
    AppDynamicsCVConfig appDynamicsCVConfig = (AppDynamicsCVConfig) result.getAdded().get(0);
    assertThat(appDynamicsCVConfig.getMetricPack()).isEqualTo(metricPacks.get(0));
    assertThat(appDynamicsCVConfig.getServiceIdentifier()).isEqualTo("service2");
    assertThat(appDynamicsCVConfig.getTierName()).isEqualTo("tier2");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_checkUpdated() {
    appDynamicsDSConfig.setMetricPacks(Sets.newHashSet(metricPacks.get(0)));
    appDynamicsDSConfig.setServiceMappings(Sets.newHashSet(
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service1").tierName("tier1").build(),
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service2").tierName("tier2").build()));
    List<CVConfig> cvConfigs = new ArrayList<>();
    cvConfigs.add(create("service1", metricPacks.get(0), "tier1"));
    CVConfigUpdateResult result = appDynamicsDSConfig.getCVConfigUpdateResult(cvConfigs);
    assertThat(result.getUpdated()).hasSize(1);
    AppDynamicsCVConfig appDynamicsCVConfig = (AppDynamicsCVConfig) result.getUpdated().get(0);
    assertThat(appDynamicsCVConfig.getMetricPack()).isEqualTo(metricPacks.get(0));
    assertThat(appDynamicsCVConfig.getServiceIdentifier()).isEqualTo("service1");
    assertThat(appDynamicsCVConfig.getTierName()).isEqualTo("tier1");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void getCVConfigUpdateResult_existingConfigOneEach() {
    appDynamicsDSConfig.setMetricPacks(Sets.newHashSet(metricPacks.get(0), metricPacks.get(1)));
    appDynamicsDSConfig.setServiceMappings(Sets.newHashSet(
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service1").tierName("tier1").build(),
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("service2").tierName("tier2").build()));
    List<CVConfig> cvConfigs = new ArrayList<>();
    cvConfigs.add(create("service1", metricPacks.get(0), "tier1"));
    cvConfigs.add(create("service1", metricPacks.get(2), "tier1"));
    cvConfigs.add(create("service2", metricPacks.get(0), "tier2"));
    cvConfigs.add(create("service2", metricPacks.get(2), "tier2"));

    CVConfigUpdateResult result = appDynamicsDSConfig.getCVConfigUpdateResult(cvConfigs);
    assertThat(result.getUpdated()).hasSize(2);
    assertThat(result.getDeleted()).hasSize(2);
    assertThat(result.getAdded()).hasSize(2);
    result.getUpdated().forEach(cvConfig -> assertThat(cvConfig.getUuid()).isNotNull());
    result.getDeleted().forEach(cvConfig -> assertThat(cvConfig.getUuid()).isNotNull());
    result.getAdded().forEach(cvConfig -> assertThat(cvConfig.getUuid()).isNull());
  }

  private CVConfig create(String serviceIdentifier, MetricPack metricPack, String tierName) {
    AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
    fillCommonFields(appDynamicsCVConfig);
    appDynamicsCVConfig.setUuid(generateUuid());
    appDynamicsCVConfig.setApplicationName(appdApplicationName);
    appDynamicsCVConfig.setServiceIdentifier(serviceIdentifier);
    appDynamicsCVConfig.setMetricPack(metricPack);
    appDynamicsCVConfig.setTierName(tierName);
    return appDynamicsCVConfig;
  }

  private MetricPack createMetricPack(int index) {
    return MetricPack.builder().accountId(accountId).identifier("metric-pack-" + index).build();
  }
}

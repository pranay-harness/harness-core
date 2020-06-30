package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.CVNextGenBaseTest;
import io.harness.cvng.core.beans.AppDynamicsDSConfig;
import io.harness.cvng.core.beans.AppDynamicsDSConfig.ServiceMapping;
import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DSConfigServiceImplTest extends CVNextGenBaseTest {
  @Inject DSConfigService dsConfigService;
  private String accountId;
  private String connectorId;
  private String productName;
  @Before
  public void setup() {
    this.accountId = generateUuid();
    this.connectorId = generateUuid();
    this.productName = "Application monitoring";
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_withSingleConfig() {
    DSConfig dsConfig = createAppDynamicsDataSourceCVConfig("appd application name");
    dsConfigService.upsert(dsConfig);
    List<? extends DSConfig> dataSourceCVConfigs =
        dsConfigService.list(accountId, connectorId, dsConfig.getProductName());
    assertThat(dataSourceCVConfigs).hasSize(1);
    AppDynamicsDSConfig appDynamicsDataSourceCVConfig = (AppDynamicsDSConfig) dataSourceCVConfigs.get(0);
    assertThat(appDynamicsDataSourceCVConfig).isEqualTo(dsConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_multiple() {
    AppDynamicsDSConfig dataSourceCVConfig = createAppDynamicsDataSourceCVConfig("app1");
    dsConfigService.upsert(dataSourceCVConfig);
    dataSourceCVConfig = createAppDynamicsDataSourceCVConfig("app2");
    dsConfigService.upsert(dataSourceCVConfig);
    List<? extends DSConfig> dataSourceCVConfigs =
        dsConfigService.list(accountId, connectorId, dataSourceCVConfig.getProductName());
    assertThat(dataSourceCVConfigs).hasSize(2);
    Set<String> identifiers = new HashSet<>();
    dataSourceCVConfigs.forEach(dsConfig -> identifiers.add(dsConfig.getIdentifier()));
    assertThat(identifiers).isEqualTo(Sets.newHashSet("app1", "app2"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete() {
    AppDynamicsDSConfig dataSourceCVConfig = createAppDynamicsDataSourceCVConfig("appd application name");
    dataSourceCVConfig.setApplicationName("app1");
    dataSourceCVConfig.setIdentifier("app1");
    dsConfigService.upsert(dataSourceCVConfig);
    assertThat(dsConfigService.list(accountId, connectorId, productName)).isNotEmpty();
    dsConfigService.delete(accountId, connectorId, productName, dataSourceCVConfig.getIdentifier());
    assertThat(dsConfigService.list(accountId, connectorId, productName)).isEmpty();
  }

  private AppDynamicsDSConfig createAppDynamicsDataSourceCVConfig(String identifier) {
    AppDynamicsDSConfig appDynamicsDSConfig = new AppDynamicsDSConfig();
    appDynamicsDSConfig.setIdentifier(identifier);
    appDynamicsDSConfig.setConnectorId(connectorId);
    appDynamicsDSConfig.setApplicationName(identifier);
    appDynamicsDSConfig.setProductName(productName);
    appDynamicsDSConfig.setEnvIdentifier("harnessProd");
    appDynamicsDSConfig.setAccountId(accountId);
    appDynamicsDSConfig.setMetricPacks(
        Sets.newHashSet(MetricPack.builder().accountId(accountId).identifier("appd performance metric pack").build()));
    appDynamicsDSConfig.setServiceMappings(
        Sets.newHashSet(ServiceMapping.builder().serviceIdentifier("harness-manager").tierName("manager").build(),
            ServiceMapping.builder().serviceIdentifier("harness-qa").tierName("manager-qa").build()));

    return appDynamicsDSConfig;
  }
}
package io.harness.cvng;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cvng.core.services.entities.MetricPack;
import io.harness.cvng.models.AppDynamicsDSConfig;
import io.harness.cvng.models.AppDynamicsDSConfig.ServiceMapping;
import io.harness.cvng.models.DSConfig;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

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
    DSConfig dsConfig = createAppDynamicsDataSourceCVConfig();
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
    AppDynamicsDSConfig dataSourceCVConfig = createAppDynamicsDataSourceCVConfig();
    dataSourceCVConfig.setApplicationName("app1");
    dataSourceCVConfig.setIdentifier("app1");
    dsConfigService.upsert(dataSourceCVConfig);
    dataSourceCVConfig = createAppDynamicsDataSourceCVConfig();
    dataSourceCVConfig.setApplicationName("app2");
    dataSourceCVConfig.setIdentifier("app2");
    dsConfigService.upsert(dataSourceCVConfig);
    List<? extends DSConfig> dataSourceCVConfigs =
        dsConfigService.list(accountId, connectorId, dataSourceCVConfig.getProductName());
    assertThat(dataSourceCVConfigs).hasSize(2);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete() {
    AppDynamicsDSConfig dataSourceCVConfig = createAppDynamicsDataSourceCVConfig();
    dataSourceCVConfig.setApplicationName("app1");
    dataSourceCVConfig.setIdentifier("app1");
    dsConfigService.upsert(dataSourceCVConfig);
    assertThat(dsConfigService.list(accountId, connectorId, productName)).isNotEmpty();
    dsConfigService.delete(accountId, connectorId, productName, dataSourceCVConfig.getIdentifier());
    assertThat(dsConfigService.list(accountId, connectorId, productName)).isEmpty();
  }

  private AppDynamicsDSConfig createAppDynamicsDataSourceCVConfig() {
    AppDynamicsDSConfig appDynamicsDSConfig = new AppDynamicsDSConfig();
    appDynamicsDSConfig.setIdentifier("appd application name");
    appDynamicsDSConfig.setConnectorId(connectorId);
    appDynamicsDSConfig.setApplicationName("appd application name");
    appDynamicsDSConfig.setProductName(productName);
    appDynamicsDSConfig.setEnvIdentifier("harnessProd");
    appDynamicsDSConfig.setAccountId(accountId);
    appDynamicsDSConfig.setMetricPacks(
        Sets.newHashSet(MetricPack.builder().accountId(accountId).name("appd performance metric pack").build()));
    appDynamicsDSConfig.setServiceMappings(
        Sets.newHashSet(ServiceMapping.builder().serviceIdentifier("harness-manager").tierName("manager").build(),
            ServiceMapping.builder().serviceIdentifier("harness-qa").tierName("manager-qa").build()));

    return appDynamicsDSConfig;
  }
}
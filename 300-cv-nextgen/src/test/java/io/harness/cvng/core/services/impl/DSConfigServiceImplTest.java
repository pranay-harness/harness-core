package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.AppDynamicsDSConfig;
import io.harness.cvng.core.beans.AppDynamicsDSConfig.ServiceMapping;
import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.beans.MonitoringSourceDTO;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.services.api.AppDynamicsService;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.core.services.api.MonitoringSourceImportStatusCreator;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class DSConfigServiceImplTest extends CvNextGenTest {
  @Inject DSConfigService dsConfigService;
  @Mock NextGenService nextGenService;
  @Mock AppDynamicsService appDynamicsService;
  @Mock private Injector injector;
  @Inject AppDynamicsCVConfigTransformer appDynamicsCVConfigTransformer;
  private String accountId;
  private String connectorIdentifier;
  private String productName;
  private String projectIdentifier;
  private String orgIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    this.accountId = generateUuid();
    this.connectorIdentifier = generateUuid();
    this.productName = "Application monitoring";
    this.projectIdentifier = "projectIdentifier";
    this.orgIdentifier = "orgIdentifier";
    FieldUtils.writeField(dsConfigService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(dsConfigService, "injector", injector, true);
    when(injector.getInstance(
             Key.get(MonitoringSourceImportStatusCreator.class, Names.named(DataSourceType.APP_DYNAMICS.name()))))
        .thenReturn(appDynamicsService);
    when(injector.getInstance(Key.get(CVConfigTransformer.class, Names.named(DataSourceType.APP_DYNAMICS.name()))))
        .thenReturn(appDynamicsCVConfigTransformer);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_withSingleConfig() {
    DSConfig dsConfig = createAppDynamicsDataSourceCVConfig("appd application name", "monitoringSourceIdentifier");
    dsConfigService.upsert(dsConfig);
    List<? extends DSConfig> dataSourceCVConfigs =
        dsConfigService.list(accountId, connectorIdentifier, dsConfig.getProductName());
    assertThat(dataSourceCVConfigs).hasSize(1);
    AppDynamicsDSConfig appDynamicsDataSourceCVConfig = (AppDynamicsDSConfig) dataSourceCVConfigs.get(0);
    assertThat(appDynamicsDataSourceCVConfig).isEqualTo(dsConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_multiple() {
    AppDynamicsDSConfig dataSourceCVConfig = createAppDynamicsDataSourceCVConfig("app1", "monitoringSourceIdentifier");
    dsConfigService.upsert(dataSourceCVConfig);
    dataSourceCVConfig = createAppDynamicsDataSourceCVConfig("app2", "monitoringSourceIdentifier");
    dsConfigService.upsert(dataSourceCVConfig);
    List<? extends DSConfig> dataSourceCVConfigs =
        dsConfigService.list(accountId, connectorIdentifier, dataSourceCVConfig.getProductName());
    assertThat(dataSourceCVConfigs).hasSize(2);
    Set<String> identifiers = new HashSet<>();
    dataSourceCVConfigs.forEach(dsConfig -> identifiers.add(dsConfig.getIdentifier()));
    assertThat(identifiers).isEqualTo(Sets.newHashSet("app1", "app2"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete() {
    AppDynamicsDSConfig dataSourceCVConfig =
        createAppDynamicsDataSourceCVConfig("appd application name", "monitoringSourceIdentifier");
    dataSourceCVConfig.setApplicationName("app1");
    dataSourceCVConfig.setIdentifier("app1");
    dsConfigService.upsert(dataSourceCVConfig);
    assertThat(dsConfigService.list(accountId, connectorIdentifier, productName)).isNotEmpty();
    dsConfigService.delete(accountId, connectorIdentifier, productName, dataSourceCVConfig.getIdentifier());
    assertThat(dsConfigService.list(accountId, connectorIdentifier, productName)).isEmpty();
  }

  private AppDynamicsDSConfig createAppDynamicsDataSourceCVConfig(
      String identifier, String monitoringSourceIdentifier) {
    AppDynamicsDSConfig appDynamicsDSConfig = new AppDynamicsDSConfig();
    appDynamicsDSConfig.setIdentifier(identifier);
    appDynamicsDSConfig.setConnectorIdentifier(connectorIdentifier);
    appDynamicsDSConfig.setApplicationName(identifier);
    appDynamicsDSConfig.setProductName(productName);
    appDynamicsDSConfig.setEnvIdentifier("harnessProd");
    appDynamicsDSConfig.setAccountId(accountId);
    appDynamicsDSConfig.setOrgIdentifier(orgIdentifier);
    appDynamicsDSConfig.setProjectIdentifier(projectIdentifier);
    appDynamicsDSConfig.setMetricPacks(
        Sets.newHashSet(MetricPack.builder().accountId(accountId).identifier("appd performance metric pack").build()));
    appDynamicsDSConfig.setServiceMappings(
        Sets.newHashSet(ServiceMapping.builder().serviceIdentifier("harness-manager").tierName("manager").build(),
            ServiceMapping.builder().serviceIdentifier("harness-qa").tierName("manager-qa").build()));
    appDynamicsDSConfig.setMonitoringSourceIdentifier(monitoringSourceIdentifier);

    return appDynamicsDSConfig;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testListMonitoringSources_checkThatNumberOfElementsAreCorrects() {
    AppDynamicsDSConfig dataSourceCVConfig1 =
        createAppDynamicsDataSourceCVConfig("appd application name 1", "monitoringSourceIdentifier 1");
    dsConfigService.upsert(dataSourceCVConfig1);
    AppDynamicsDSConfig dataSourceCVConfig2 =
        createAppDynamicsDataSourceCVConfig("appd application name 2", "monitoringSourceIdentifier 2");
    dsConfigService.upsert(dataSourceCVConfig2);
    AppDynamicsDSConfig dataSourceCVConfig3 =
        createAppDynamicsDataSourceCVConfig("appd application name 3", "monitoringSourceIdentifier 3");
    dsConfigService.upsert(dataSourceCVConfig3);
    AppDynamicsDSConfig dataSourceCVConfig4 =
        createAppDynamicsDataSourceCVConfig("appd application name 4", "monitoringSourceIdentifier 3");
    dsConfigService.upsert(dataSourceCVConfig4);
    List<MonitoringSourceDTO> monitoringSourceDTOS =
        dsConfigService.listMonitoringSources(accountId, orgIdentifier, projectIdentifier, 10, 0);
    assertThat(monitoringSourceDTOS.size()).isEqualTo(3);
    assertThat(monitoringSourceDTOS.get(0).getMonitoringSourceIdentifier()).isEqualTo("monitoringSourceIdentifier 3");
    assertThat(monitoringSourceDTOS.get(1).getMonitoringSourceIdentifier()).isEqualTo("monitoringSourceIdentifier 2");
    assertThat(monitoringSourceDTOS.get(2).getMonitoringSourceIdentifier()).isEqualTo("monitoringSourceIdentifier 1");
  }

  private ConnectorInfoDTO createAppdynamicsConnector() {
    return ConnectorInfoDTO.builder().connectorConfig(AppDynamicsConnectorDTO.builder().build()).build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testListMonitoringSources_checkThatNumberOfServicesAreCorrectForAppdynamics() {
    AppDynamicsDSConfig dataSourceCVConfig1 =
        createAppDynamicsDataSourceCVConfig("appd application name 1", "monitoringSourceIdentifier 1");
    dataSourceCVConfig1.setApplicationName("application 1");
    dataSourceCVConfig1.setEnvIdentifier("env 1");
    dataSourceCVConfig1.setServiceMappings(
        Sets.newHashSet(ServiceMapping.builder().serviceIdentifier("harness-manager").tierName("manager").build(),
            ServiceMapping.builder().serviceIdentifier("harness-qa").tierName("manager-qa").build()));
    dsConfigService.upsert(dataSourceCVConfig1);

    AppDynamicsDSConfig dataSourceCVConfig2 =
        createAppDynamicsDataSourceCVConfig("appd application name 2", "monitoringSourceIdentifier 1");
    dataSourceCVConfig2.setApplicationName("application 2");
    dataSourceCVConfig2.setEnvIdentifier("env 2");
    dataSourceCVConfig2.setServiceMappings(
        Sets.newHashSet(ServiceMapping.builder().serviceIdentifier("harness-manager-dev").tierName("manager").build(),
            ServiceMapping.builder().serviceIdentifier("harness-qa").tierName("manager-qa").build()));
    dsConfigService.upsert(dataSourceCVConfig2);

    List<MonitoringSourceDTO> monitoringSourceDTOS =
        dsConfigService.listMonitoringSources(accountId, orgIdentifier, projectIdentifier, 10, 0);
    assertThat(monitoringSourceDTOS.size()).isEqualTo(1);
    MonitoringSourceDTO monitoringSourceDTO = monitoringSourceDTOS.get(0);
    assertThat(monitoringSourceDTO.getMonitoringSourceIdentifier()).isEqualTo("monitoringSourceIdentifier 1");
    assertThat(monitoringSourceDTO.getNumberOfServices()).isEqualTo(3);
  }
}

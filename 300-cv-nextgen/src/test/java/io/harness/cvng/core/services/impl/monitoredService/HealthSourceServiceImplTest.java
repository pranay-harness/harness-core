package io.harness.cvng.core.services.impl.monitoredService;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.MonitoredServiceDataSourceType;
import io.harness.cvng.core.beans.monitoredService.HealthSource;
import io.harness.cvng.core.beans.monitoredService.MetricPackDTO;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceSpec;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.monitoredService.HealthSourceService;
import io.harness.exception.DuplicateFieldException;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HealthSourceServiceImplTest extends CvNextGenTestBase {
  @Inject HealthSourceService healthSourceService;
  @Inject MetricPackService metricPackService;
  @Inject CVConfigService cvConfigService;

  String identifier;
  String name;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String environmentIdentifier;
  String serviceIdentifier;
  String feature;
  String connectorIdentifier;
  String appTierName;
  String applicationName;
  String nameSpaceIdentifier;
  @Before
  public void setup() {
    identifier = "health-source-identifier";
    name = "health-source-name";
    accountId = generateUuid();
    orgIdentifier = "org";
    projectIdentifier = "project";
    environmentIdentifier = "env";
    serviceIdentifier = "service";
    feature = "Application Monitoring";
    connectorIdentifier = "connectorIdentifier";
    applicationName = "applicationName";
    appTierName = "appTierName";
    nameSpaceIdentifier = "monitoredServiceIdentifier";
    metricPackService.createDefaultMetricPackAndThresholds(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCheckIfAlreadyPresent_ExistingConfigsWithIdentifier() {
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSourceService.create(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource), true);
    assertThatThrownBy(()
                           -> healthSourceService.checkIfAlreadyPresent(accountId, orgIdentifier, projectIdentifier,
                               nameSpaceIdentifier, Sets.newHashSet(healthSource)))
        .isInstanceOf(DuplicateFieldException.class)
        .hasMessage(String.format(
            "Already Existing configs for Monitored Service  with identifier %s and orgIdentifier %s and projectIdentifier %s",
            HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()),
            orgIdentifier, projectIdentifier));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testCreate_CVConfigsCreation() {
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSourceService.create(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource), true);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()));
    assertThat(cvConfigs.size()).isEqualTo(1);
    AppDynamicsCVConfig cvConfig = (AppDynamicsCVConfig) cvConfigs.get(0);
    commonCVConfigAssert(cvConfig);
    assertThat(cvConfig.getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
    assertThat(cvConfig.isEnabled()).isTrue();
    assertThat(cvConfig.getMetricPack())
        .isEqualTo(metricPackService.getMetricPack(
            accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS, CVMonitoringCategory.ERRORS));
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testGet() {
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSourceService.create(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource), false);
    Set<HealthSource> savedHealthSources = healthSourceService.get(
        accountId, orgIdentifier, projectIdentifier, nameSpaceIdentifier, Arrays.asList(identifier));
    assertThat(savedHealthSources.size()).isEqualTo(1);
    HealthSource saveHealthSource = savedHealthSources.iterator().next();
    assertThat(saveHealthSource).isEqualTo(healthSource);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testDelete() {
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSourceService.create(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource), true);
    healthSourceService.delete(
        accountId, orgIdentifier, projectIdentifier, nameSpaceIdentifier, Arrays.asList(identifier));
    List<CVConfig> cvConfigs =
        cvConfigService.list(accountId, orgIdentifier, projectIdentifier, healthSource.getIdentifier());
    assertThat(cvConfigs.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_updateName() {
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSourceService.create(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource), true);
    healthSource.setName("new-name");
    healthSourceService.update(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource));
    Set<HealthSource> savedHealthSource = healthSourceService.get(
        accountId, orgIdentifier, projectIdentifier, nameSpaceIdentifier, Arrays.asList(identifier));
    assertThat(savedHealthSource.size()).isEqualTo(1);
    assertThat(savedHealthSource.iterator().next().getName()).isEqualTo("new-name");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_deleteAndAddCVConfigs() {
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSourceService.create(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource), true);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()));
    assertThat(cvConfigs.size()).isEqualTo(1);
    AppDynamicsCVConfig cvConfig = (AppDynamicsCVConfig) cvConfigs.get(0);
    commonCVConfigAssert(cvConfig);
    assertThat(cvConfig.getMetricPack().getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);

    HealthSource updatedHealthSource = createHealthSource(CVMonitoringCategory.PERFORMANCE);
    healthSourceService.update(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(updatedHealthSource));
    cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()));
    assertThat(cvConfigs.size()).isEqualTo(1);
    cvConfig = (AppDynamicsCVConfig) cvConfigs.get(0);
    commonCVConfigAssert(cvConfig);
    assertThat(cvConfig.getMetricPack().getCategory()).isEqualTo(CVMonitoringCategory.PERFORMANCE);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_updateCVConfigs() {
    HealthSource healthSource = createHealthSource(CVMonitoringCategory.ERRORS);
    healthSourceService.create(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource), true);
    healthSource.setIdentifier("new-identifier");
    healthSource.setName("new-name");
    healthSourceService.update(accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier,
        nameSpaceIdentifier, Sets.newHashSet(healthSource));
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, orgIdentifier, projectIdentifier,
        HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, healthSource.getIdentifier()));
    assertThat(cvConfigs.size()).isEqualTo(1);
    AppDynamicsCVConfig cvConfig = (AppDynamicsCVConfig) cvConfigs.get(0);
    assertThat(cvConfig.getIdentifier())
        .isEqualTo(HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, "new-identifier"));
    assertThat(cvConfig.getMonitoringSourceName()).isEqualTo("new-name");
    assertThat(cvConfig.getMetricPack().getCategory()).isEqualTo(CVMonitoringCategory.ERRORS);
  }

  HealthSource createHealthSource(CVMonitoringCategory cvMonitoringCategory) {
    HealthSourceSpec healthSourceSpec =
        AppDynamicsHealthSourceSpec.builder()
            .applicationName(applicationName)
            .tierName(appTierName)
            .connectorRef(connectorIdentifier)
            .feature(feature)
            .metricPacks(Arrays.asList(MetricPackDTO.builder().identifier(cvMonitoringCategory).build())
                             .stream()
                             .collect(Collectors.toSet()))
            .build();
    return HealthSource.builder()
        .identifier(identifier)
        .name(name)
        .type(MonitoredServiceDataSourceType.APP_DYNAMICS)
        .spec(healthSourceSpec)
        .build();
  }

  private void commonCVConfigAssert(AppDynamicsCVConfig cvConfig) {
    assertThat(cvConfig.getAccountId()).isEqualTo(accountId);
    assertThat(cvConfig.getOrgIdentifier()).isEqualTo(orgIdentifier);
    assertThat(cvConfig.getProjectIdentifier()).isEqualTo(projectIdentifier);
    assertThat(cvConfig.getEnvIdentifier()).isEqualTo(environmentIdentifier);
    assertThat(cvConfig.getServiceIdentifier()).isEqualTo(serviceIdentifier);
    assertThat(cvConfig.getProductName()).isEqualTo(feature);
    assertThat(cvConfig.getMonitoringSourceName()).isEqualTo(name);
    assertThat(cvConfig.getConnectorIdentifier()).isEqualTo(connectorIdentifier);
    assertThat(cvConfig.getTierName()).isEqualTo(appTierName);
    assertThat(cvConfig.getApplicationName()).isEqualTo(applicationName);
    assertThat(cvConfig.getIdentifier())
        .isEqualTo(HealthSourceService.getNameSpacedIdentifier(nameSpaceIdentifier, identifier));
  }
}

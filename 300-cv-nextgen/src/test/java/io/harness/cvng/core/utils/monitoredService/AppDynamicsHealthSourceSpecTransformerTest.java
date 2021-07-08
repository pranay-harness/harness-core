package io.harness.cvng.core.utils.monitoredService;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.AppDynamicsHealthSourceSpec;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppDynamicsHealthSourceSpecTransformerTest extends CvNextGenTestBase {
  Set<MetricPack> metricPacks;
  String applicationName;
  String envIdentifier;
  String connectorIdentifier;
  String productName;
  String projectIdentifier;
  String accountId;
  String identifier;
  String monitoringSourceName;
  String serviceIdentifier;
  String tierName;

  @Inject AppDynamicsHealthSourceSpecTransformer appDynamicsHealthSourceSpecTransformer;

  @Before
  public void setup() {
    metricPacks =
        Sets.newHashSet(MetricPack.builder().identifier("Errors").category(CVMonitoringCategory.ERRORS).build(),
            MetricPack.builder().identifier("Performance").category(CVMonitoringCategory.PERFORMANCE).build());
    applicationName = "appName";
    envIdentifier = "env";
    connectorIdentifier = "connectorId";
    productName = "Application Monitoring";
    projectIdentifier = "projectId";
    accountId = generateUuid();
    identifier = "healthSourceIdentifier";
    monitoringSourceName = "AppDynamics";
    serviceIdentifier = "serviceId";
    tierName = "tierName";
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void transformToDSConfig_preconditionEmptyCVConfigs() {
    assertThatThrownBy(() -> appDynamicsHealthSourceSpecTransformer.transform(Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("List of cvConfigs can not empty.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void transformToDSConfig_preconditionDifferentIdentifier() {
    List<AppDynamicsCVConfig> cvConfigs = createCVConfigs();
    cvConfigs.get(0).setIdentifier("different-identifier");
    assertThatThrownBy(() -> appDynamicsHealthSourceSpecTransformer.transform(cvConfigs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Group ID should be same for List of all configs.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig_preconditionForApplicationName() {
    List<AppDynamicsCVConfig> cvConfigs = createCVConfigs();
    cvConfigs.get(0).setApplicationName("different-application-name");
    assertThatThrownBy(() -> appDynamicsHealthSourceSpecTransformer.transform(cvConfigs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Application Name should be same for List of all configs.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig_preconditionForConnectorRef() {
    List<AppDynamicsCVConfig> cvConfigs = createCVConfigs();
    cvConfigs.get(0).setConnectorIdentifier("different-connector-ref");
    assertThatThrownBy(() -> appDynamicsHealthSourceSpecTransformer.transform(cvConfigs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ConnectorRef should be same for List of all configs.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig_preconditionForApplicationTierName() {
    List<AppDynamicsCVConfig> cvConfigs = createCVConfigs();
    cvConfigs.get(0).setTierName("different-tier-name");
    assertThatThrownBy(() -> appDynamicsHealthSourceSpecTransformer.transform(cvConfigs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Application tier name should be same for List of all configs.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig_preconditionForFeatureName() {
    List<AppDynamicsCVConfig> cvConfigs = createCVConfigs();
    cvConfigs.get(0).setProductName("different-product-name");
    assertThatThrownBy(() -> appDynamicsHealthSourceSpecTransformer.transform(cvConfigs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Application feature name should be same for List of all configs.");
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testTransformToHealthSourceConfig() {
    List<AppDynamicsCVConfig> cvConfigs = createCVConfigs();
    AppDynamicsHealthSourceSpec appDynamicsHealthSourceSpec =
        appDynamicsHealthSourceSpecTransformer.transform(cvConfigs);

    assertThat(appDynamicsHealthSourceSpec.getAppdApplicationName()).isEqualTo(applicationName);
    assertThat(appDynamicsHealthSourceSpec.getConnectorRef()).isEqualTo(connectorIdentifier);
    assertThat(appDynamicsHealthSourceSpec.getAppdTierName()).isEqualTo(tierName);
    assertThat(appDynamicsHealthSourceSpec.getFeature()).isEqualTo(productName);
    assertThat(appDynamicsHealthSourceSpec.getMetricPacks().size()).isEqualTo(2);
  }

  private List<AppDynamicsCVConfig> createCVConfigs() {
    List<AppDynamicsCVConfig> appDynamicsCVConfigs = new ArrayList<>();
    metricPacks.forEach(metricPack
        -> appDynamicsCVConfigs.add(AppDynamicsCVConfig.builder()
                                        .applicationName(applicationName)
                                        .envIdentifier(envIdentifier)
                                        .connectorIdentifier(connectorIdentifier)
                                        .productName(productName)
                                        .projectIdentifier(projectIdentifier)
                                        .accountId(accountId)
                                        .identifier(identifier)
                                        .monitoringSourceName(monitoringSourceName)
                                        .serviceIdentifier(serviceIdentifier)
                                        .tierName(tierName)
                                        .metricPack(metricPack)
                                        .build()));
    return appDynamicsCVConfigs;
  }
}

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.AppDynamicsDSConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DSConfigService;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;
import io.harness.cvng.models.VerificationType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CVConfigServiceImplTest extends CvNextGenTest {
  @Inject private CVConfigService cvConfigService;
  @Inject private DSConfigService dsConfigService;
  @Mock private NextGenService nextGenService;
  private String accountId;
  private String connectorIdentifier;
  private String productName;
  private String groupId;
  private String serviceInstanceIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    connectorIdentifier = generateUuid();
    productName = generateUuid();
    groupId = generateUuid();
    serviceInstanceIdentifier = generateUuid();
    projectIdentifier = generateUuid();
    orgIdentifier = generateUuid();
    when(nextGenService.getEnvironment(anyString(), anyString(), anyString(), anyString())).then(invocation -> {
      Object[] args = invocation.getArguments();
      return EnvironmentResponseDTO.builder()
          .identifier((String) args[0])
          .accountId((String) args[1])
          .orgIdentifier((String) args[2])
          .projectIdentifier((String) args[3])
          .name((String) args[0])
          .build();
    });
    when(nextGenService.getService(anyString(), anyString(), anyString(), anyString())).then(invocation -> {
      Object[] args = invocation.getArguments();
      return ServiceResponseDTO.builder()
          .identifier((String) args[0])
          .accountId((String) args[1])
          .orgIdentifier((String) args[2])
          .projectIdentifier((String) args[3])
          .name((String) args[0])
          .build();
    });
    FieldUtils.writeField(cvConfigService, "nextGenService", nextGenService, true);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave() {
    CVConfig cvConfig = createCVConfig();
    CVConfig updated = save(cvConfig);
    CVConfig saved = cvConfigService.get(updated.getUuid());
    assertCommons(saved, cvConfig);
  }

  private CVConfig save(CVConfig cvConfig) {
    return cvConfigService.save(cvConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave_batchAPI() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    save(cvConfigs);
    cvConfigs.forEach(cvConfig -> assertCommons(cvConfigService.get(cvConfig.getUuid()), cvConfig));
  }

  private List<CVConfig> save(List<CVConfig> cvConfigs) {
    return cvConfigService.save(cvConfigs);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testSave_batchAPIIfUUIDIsDefined() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    cvConfigs.forEach(cvConfig -> cvConfig.setUuid(generateUuid()));
    assertThatThrownBy(() -> save(cvConfigs))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("UUID should be null when creating CVConfig");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGet() {
    CVConfig cvConfig = createCVConfig();
    CVConfig updated = save(cvConfig);
    CVConfig saved = cvConfigService.get(updated.getUuid());
    assertCommons(saved, cvConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFind_filterByAccountAndDataSourceTypesIfExist() {
    CVConfig cvConfig = createCVConfig();
    CVConfig updated = save(cvConfig);
    List<CVConfig> results = cvConfigService.find(
        accountId, orgIdentifier, projectIdentifier, "service", "env", Lists.newArrayList(DataSourceType.SPLUNK));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getUuid()).isEqualTo(updated.getUuid());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFind_filterByAccountAndDataSourceTypesIfDoesNotExist() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    List<CVConfig> results = cvConfigService.find(
        accountId, orgIdentifier, projectIdentifier, "service", "env", Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    assertThat(results).hasSize(0);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete() {
    CVConfig cvConfig = createCVConfig();
    CVConfig updated = save(cvConfig);
    cvConfigService.delete(updated.getUuid());
    assertThat(cvConfigService.get(cvConfig.getUuid())).isEqualTo(null);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdate_withMultipleCVConfig() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    CVConfig updated = cvConfigService.get(cvConfig.getUuid());
    updated.setProjectIdentifier("ProjectIdentifier");
    cvConfigService.update(Lists.newArrayList(updated));
    assertCommons(cvConfigService.get(updated.getUuid()), updated);
    assertThat(updated.getProjectIdentifier()).isEqualTo("ProjectIdentifier");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdate_withEmptyCVConfigId() {
    CVConfig cvConfig = createCVConfig();
    assertThatThrownBy(() -> cvConfigService.update(Lists.newArrayList(cvConfig)))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Trying to update a CVConfig with empty UUID.");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_findSingleCVConfig() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, cvConfig.getConnectorIdentifier());
    assertCommons(cvConfigs.get(0), cvConfig);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_zeroMatch() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, generateUuid());
    assertThat(cvConfigs).hasSize(0);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_multipleMatchMultipleConnectorIdentifiers() {
    List<CVConfig> cvConfigs1 = createCVConfigs(5);
    String connectorIdentifier1 = generateUuid();
    cvConfigs1.forEach(cvConfig -> {
      cvConfig.setConnectorIdentifier(connectorIdentifier1);
      save(cvConfig);
    });

    List<CVConfig> cvConfigs2 = createCVConfigs(7);
    String connectorIdentifier2 = generateUuid();
    cvConfigs2.forEach(cvConfig -> {
      cvConfig.setConnectorIdentifier(connectorIdentifier2);
      save(cvConfig);
    });

    assertThat(cvConfigService.list(accountId, connectorIdentifier1)).hasSize(5);
    assertThat(cvConfigService.list(accountId, connectorIdentifier2)).hasSize(7);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_withConnectorAndProductName() {
    List<CVConfig> cvConfigs = createCVConfigs(4);
    String connectorIdentifier1 = generateUuid();
    cvConfigs.forEach(cvConfig -> {
      cvConfig.setConnectorIdentifier(connectorIdentifier1);
      cvConfig.setProductName("product1");
    });
    cvConfigs.get(0).setProductName("product2");
    save(cvConfigs);
    assertThat(cvConfigService.list(accountId, connectorIdentifier1, "product1")).hasSize(3);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_withConnectorAndProductNameGroupId() {
    List<CVConfig> cvConfigs = createCVConfigs(4);
    String connectorIdentifier1 = generateUuid();
    cvConfigs.forEach(cvConfig -> {
      cvConfig.setConnectorIdentifier(connectorIdentifier1);
      cvConfig.setProductName("product1");
      cvConfig.setGroupId("group1");
    });
    cvConfigs.get(0).setProductName("product2");
    save(cvConfigs);
    assertThat(cvConfigService.list(accountId, connectorIdentifier1, "product1", "group1")).hasSize(3);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testList_withServiceEnvironmentCategory() {
    List<CVConfig> cvConfigs = createCVConfigs(4);
    String serviceIdentifier = generateUuid();
    String envIdentifier = generateUuid();
    CVMonitoringCategory category = CVMonitoringCategory.PERFORMANCE;
    int index = 0;
    for (CVConfig cvConfig : cvConfigs) {
      cvConfig.setOrgIdentifier(orgIdentifier);
      cvConfig.setProjectIdentifier(projectIdentifier);
      cvConfig.setServiceIdentifier(serviceIdentifier);
      cvConfig.setEnvIdentifier(envIdentifier);
      cvConfig.setCategory(index++ % 2 == 0 ? CVMonitoringCategory.PERFORMANCE : CVMonitoringCategory.ERRORS);
    }
    save(cvConfigs);
    assertThat(cvConfigService.list(accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier,
                   CVMonitoringCategory.PERFORMANCE))
        .hasSize(2);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testList_withNullCategory() {
    List<CVConfig> cvConfigs = createCVConfigs(4);
    String serviceIdentifier = generateUuid();
    String envIdentifier = generateUuid();
    CVMonitoringCategory category = CVMonitoringCategory.PERFORMANCE;
    int index = 0;
    for (CVConfig cvConfig : cvConfigs) {
      cvConfig.setOrgIdentifier(orgIdentifier);
      cvConfig.setProjectIdentifier(projectIdentifier);
      cvConfig.setServiceIdentifier(serviceIdentifier);
      cvConfig.setEnvIdentifier(envIdentifier);
      cvConfig.setCategory(index++ % 2 == 0 ? CVMonitoringCategory.PERFORMANCE : CVMonitoringCategory.ERRORS);
    }
    save(cvConfigs);
    assertThat(
        cvConfigService.list(accountId, orgIdentifier, projectIdentifier, envIdentifier, serviceIdentifier, null))
        .hasSize(4);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetProjectsNames_whenNoConfigsPresent() {
    assertThat(cvConfigService.getProductNames(accountId, generateUuid())).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetProjectsNames_withMultipleDuplicateProjectNames() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    List<String> projectNames = Arrays.asList("p2", "p1", "p2", "p3", "p3");
    IntStream.range(0, 5).forEach(index -> cvConfigs.get(index).setProductName(projectNames.get(index)));
    save(cvConfigs);
    assertThat(cvConfigService.getProductNames(accountId, connectorIdentifier))
        .isEqualTo(Lists.newArrayList("p1", "p2", "p3"));
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDeleteByGroupId() {
    String groupName = "appdynamics-app-name";
    List<CVConfig> cvConfigs = createCVConfigs(5);
    cvConfigs.forEach(cvConfig -> cvConfig.setGroupId(groupName));
    save(cvConfigs);
    cvConfigService.deleteByGroupId(accountId, connectorIdentifier, productName, groupName);
    cvConfigs.forEach(cvConfig -> assertThat(cvConfigService.get(cvConfig.getUuid())).isNull());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void setCollectionTaskId() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    assertThat(cvConfig.getPerpetualTaskId()).isNull();
    String taskId = generateUuid();
    cvConfigService.setCollectionTaskId(cvConfig.getUuid(), taskId);
    CVConfig updated = cvConfigService.get(cvConfig.getUuid());
    assertThat(updated.getPerpetualTaskId()).isEqualTo(taskId);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testGetEnvToServicesList() {
    int numOfEnv = 3;
    for (int i = 0; i < numOfEnv; i++) {
      AppDynamicsDSConfig dataSourceCVConfig = createAppDynamicsDataSourceCVConfig("appd-" + i);
      dataSourceCVConfig.setEnvIdentifier("env-" + i);
      dsConfigService.upsert(dataSourceCVConfig);
    }

    List<EnvToServicesDTO> envToServicesList =
        cvConfigService.getEnvToServicesList(accountId, orgIdentifier, projectIdentifier);
    assertThat(envToServicesList.size()).isEqualTo(numOfEnv);

    for (int i = 0; i < numOfEnv; i++) {
      EnvToServicesDTO envToServicesDTO = envToServicesList.get(i);
      int envIndex = numOfEnv - i - 1;
      assertThat(envToServicesDTO.getEnvironment().getIdentifier()).isEqualTo("env-" + envIndex);
      assertThat(envToServicesDTO.getEnvironment().getName()).isEqualTo("env-" + envIndex);
      assertThat(envToServicesDTO.getEnvironment().getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(envToServicesDTO.getEnvironment().getOrgIdentifier()).isEqualTo(orgIdentifier);
      assertThat(envToServicesDTO.getEnvironment().getAccountId()).isEqualTo(accountId);

      assertThat(envToServicesDTO.getServices().size()).isEqualTo(2);
      List<ServiceResponseDTO> services = new ArrayList<>(envToServicesDTO.getServices());
      ServiceResponseDTO serviceResponseDTO = services.get(0);
      assertThat(Sets.newHashSet("harness-qa", "harness-manager")).containsOnlyOnce(serviceResponseDTO.getIdentifier());
      assertThat(Sets.newHashSet("harness-qa", "harness-manager")).containsOnlyOnce(serviceResponseDTO.getName());
      assertThat(serviceResponseDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(serviceResponseDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
      assertThat(serviceResponseDTO.getAccountId()).isEqualTo(accountId);

      serviceResponseDTO = services.get(1);
      assertThat(Sets.newHashSet("harness-qa", "harness-manager")).containsOnlyOnce(serviceResponseDTO.getIdentifier());
      assertThat(Sets.newHashSet("harness-qa", "harness-manager")).containsOnlyOnce(serviceResponseDTO.getName());
      assertThat(serviceResponseDTO.getProjectIdentifier()).isEqualTo(projectIdentifier);
      assertThat(serviceResponseDTO.getOrgIdentifier()).isEqualTo(orgIdentifier);
      assertThat(serviceResponseDTO.getAccountId()).isEqualTo(accountId);
    }
  }
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetAvailableCategories() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    Set<CVMonitoringCategory> categories =
        cvConfigService.getAvailableCategories(accountId, orgIdentifier, projectIdentifier);
    assertThat(categories).isEqualTo(Sets.newHashSet(CVMonitoringCategory.PERFORMANCE));
  }

  private AppDynamicsDSConfig createAppDynamicsDataSourceCVConfig(String identifier) {
    AppDynamicsDSConfig appDynamicsDSConfig = new AppDynamicsDSConfig();
    appDynamicsDSConfig.setIdentifier(identifier);
    appDynamicsDSConfig.setConnectorIdentifier(connectorIdentifier);
    appDynamicsDSConfig.setApplicationName(identifier);
    appDynamicsDSConfig.setProductName(productName);
    appDynamicsDSConfig.setEnvIdentifier("env");
    appDynamicsDSConfig.setAccountId(accountId);
    appDynamicsDSConfig.setProjectIdentifier(projectIdentifier);
    appDynamicsDSConfig.setOrgIdentifier(orgIdentifier);
    appDynamicsDSConfig.setMetricPacks(
        Sets.newHashSet(MetricPack.builder().accountId(accountId).identifier("appd performance metric pack").build()));
    appDynamicsDSConfig.setServiceMappings(Sets.newHashSet(
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("harness-manager").tierName("manager").build(),
        AppDynamicsDSConfig.ServiceMapping.builder().serviceIdentifier("harness-qa").tierName("manager-qa").build()));

    return appDynamicsDSConfig;
  }

  private void assertCommons(CVConfig actual, CVConfig expected) {
    assertThat(actual.getVerificationType()).isEqualTo(expected.getVerificationType());
    assertThat(actual.getAccountId()).isEqualTo(expected.getAccountId());
    assertThat(actual.getConnectorIdentifier()).isEqualTo(expected.getConnectorIdentifier());
    assertThat(actual.getServiceIdentifier()).isEqualTo(expected.getServiceIdentifier());
    assertThat(actual.getEnvIdentifier()).isEqualTo(expected.getEnvIdentifier());
    assertThat(actual.getProjectIdentifier()).isEqualTo(expected.getProjectIdentifier());
    assertThat(actual.getGroupId()).isEqualTo(expected.getGroupId());
    assertThat(actual.getCategory()).isEqualTo(expected.getCategory());
    assertThat(actual.getProductName()).isEqualTo(expected.getProductName());
    assertThat(actual.getType()).isEqualTo(expected.getType());
  }

  public List<CVConfig> createCVConfigs(int n) {
    return IntStream.range(0, n).mapToObj(index -> createCVConfig()).collect(Collectors.toList());
  }

  private CVConfig createCVConfig() {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(serviceInstanceIdentifier);
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setGroupId(groupId);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(productName);
  }
}

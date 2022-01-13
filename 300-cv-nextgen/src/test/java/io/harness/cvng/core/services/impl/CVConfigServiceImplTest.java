/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.VerificationManagerService;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.models.VerificationType;
import io.harness.encryption.Scope;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class CVConfigServiceImplTest extends CvNextGenTestBase {
  @Inject private CVConfigService cvConfigService;
  @Mock private NextGenService nextGenService;

  @Mock private VerificationManagerService verificationManagerService;

  private String accountId;
  private String connectorIdentifier;
  private String productName;
  private String groupId;
  private String serviceInstanceIdentifier;
  private String projectIdentifier;
  private String orgIdentifier;
  private String monitoringSourceIdentifier;
  private String monitoringSourceName;
  private String environmentIdentifier;
  BuilderFactory builderFactory;
  ServiceEnvironmentParams serviceEnvironmentParams;

  @Before
  public void setup() throws IllegalAccessException {
    builderFactory = BuilderFactory.getDefault();
    accountId = builderFactory.getContext().getAccountId();
    connectorIdentifier = generateUuid();
    productName = generateUuid();
    groupId = generateUuid();
    serviceInstanceIdentifier = generateUuid();
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    environmentIdentifier = generateUuid();
    monitoringSourceIdentifier = generateUuid();
    monitoringSourceName = generateUuid();
    serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                   .accountIdentifier(accountId)
                                   .projectIdentifier(projectIdentifier)
                                   .orgIdentifier(orgIdentifier)
                                   .serviceIdentifier(serviceInstanceIdentifier)
                                   .environmentIdentifier(environmentIdentifier)
                                   .build();
    when(nextGenService.getEnvironment(anyString(), anyString(), anyString(), anyString())).then(invocation -> {
      Object[] args = invocation.getArguments();
      return EnvironmentResponseDTO.builder()
          .accountId((String) args[0])
          .orgIdentifier((String) args[1])
          .projectIdentifier((String) args[2])
          .identifier((String) args[3])
          .name((String) args[3])
          .type(EnvironmentType.Production)
          .build();
    });
    when(nextGenService.getService(anyString(), anyString(), anyString(), anyString())).then(invocation -> {
      Object[] args = invocation.getArguments();
      return ServiceResponseDTO.builder()
          .accountId((String) args[0])
          .orgIdentifier((String) args[1])
          .projectIdentifier((String) args[2])
          .identifier((String) args[3])
          .name((String) args[3])
          .build();
    });
    FieldUtils.writeField(cvConfigService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(cvConfigService, "verificationManagerService", verificationManagerService, true);
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
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testUpdate_AppDCVConfig() {
    AppDynamicsCVConfig appDynamicsCVConfig = createAppDCVConfig();
    save(appDynamicsCVConfig);
    AppDynamicsCVConfig updated = (AppDynamicsCVConfig) cvConfigService.get(appDynamicsCVConfig.getUuid());
    updated.setTierName("updated-tier-name");
    updated.setApplicationName("updated-application-name");
    cvConfigService.update(updated);
    AppDynamicsCVConfig updateStored = (AppDynamicsCVConfig) cvConfigService.get(updated.getUuid());
    assertCommons(updated, updateStored);
    assertThat(updateStored.getApplicationName()).isEqualTo("updated-application-name");
    assertThat(updateStored.getTierName()).isEqualTo("updated-tier-name");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpdate_withMultipleCVConfig() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    CVConfig updated = cvConfigService.get(cvConfig.getUuid());
    updated.setEnvIdentifier("envIdentifier");
    cvConfigService.update(Lists.newArrayList(updated));
    assertCommons(cvConfigService.get(updated.getUuid()), updated);
    assertThat(updated.getEnvIdentifier()).isEqualTo("envIdentifier");
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
  public void testList_withAccountOrgAndProjectId() {
    List<CVConfig> cvConfigs = createCVConfigs(4);
    cvConfigs.forEach(cvConfig -> {
      cvConfig.setOrgIdentifier(orgIdentifier);
      cvConfig.setProjectIdentifier(projectIdentifier);
      cvConfig.setIdentifier("group1");
      cvConfig.setMonitoringSourceName("group1");
    });
    cvConfigs.get(0).setProjectIdentifier("project2");
    save(cvConfigs);
    assertThat(cvConfigService.list(accountId, orgIdentifier, projectIdentifier, "group1")).hasSize(3);
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
    cvConfigs.forEach(cvConfig -> cvConfig.setIdentifier(groupName));
    save(cvConfigs);
    cvConfigService.deleteByIdentifier(accountId, orgIdentifier, projectIdentifier, groupName);
    cvConfigs.forEach(cvConfig -> assertThat(cvConfigService.get(cvConfig.getUuid())).isNull());
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetAvailableCategories() {
    CVConfig cvConfig = createCVConfig();
    save(cvConfig);
    Set<CVMonitoringCategory> categories =
        cvConfigService.getAvailableCategories(accountId, orgIdentifier, projectIdentifier, null, null);
    assertThat(categories).isEqualTo(Sets.newHashSet(CVMonitoringCategory.PERFORMANCE));
    categories = cvConfigService.getAvailableCategories(accountId, orgIdentifier, projectIdentifier, "env", null);
    assertThat(categories).isEqualTo(Sets.newHashSet(CVMonitoringCategory.PERFORMANCE));
    categories =
        cvConfigService.getAvailableCategories(accountId, orgIdentifier, projectIdentifier, generateUuid(), null);
    assertThat(categories).isEmpty();

    categories = cvConfigService.getAvailableCategories(accountId, orgIdentifier, projectIdentifier, null, "service");
    assertThat(categories).isEqualTo(Sets.newHashSet(CVMonitoringCategory.PERFORMANCE));
    categories =
        cvConfigService.getAvailableCategories(accountId, orgIdentifier, projectIdentifier, null, generateUuid());
    assertThat(categories).isEmpty();

    categories = cvConfigService.getAvailableCategories(accountId, orgIdentifier, projectIdentifier, "env", "service");
    assertThat(categories).isEqualTo(Sets.newHashSet(CVMonitoringCategory.PERFORMANCE));
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testGetMonitoringSourceIds() {
    String MONITORING_SOURCE_SUFFIX = "Monitoring Source Id ";
    List<CVConfig> cvConfigs = createCVConfigs(50);
    for (int i = 0; i < 50; i++) {
      cvConfigs.get(i).setIdentifier(MONITORING_SOURCE_SUFFIX + i);
    }
    save(cvConfigs);

    // Testing no filter
    List<String> monitoringSourceIds =
        cvConfigService.getMonitoringSourceIds(accountId, orgIdentifier, projectIdentifier, null);
    assertThat(monitoringSourceIds.size()).isEqualTo(50);

    // Testing filter
    monitoringSourceIds =
        cvConfigService.getMonitoringSourceIds(accountId, orgIdentifier, projectIdentifier, "Ce iD   ");
    assertThat(monitoringSourceIds.size()).isEqualTo(50);

    monitoringSourceIds =
        cvConfigService.getMonitoringSourceIds(accountId, orgIdentifier, projectIdentifier, generateUuid());
    assertThat(monitoringSourceIds).isEmpty();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testlistByMonitoringSources() {
    String MONITORING_SOURCE_SUFFIX = "Monitoring Source Id ";
    List<CVConfig> cvConfigs = createCVConfigs(10);
    for (int i = 0; i < 3; i++) {
      cvConfigs.get(i).setIdentifier(MONITORING_SOURCE_SUFFIX + "0");
    }
    for (int i = 0; i < 3; i++) {
      cvConfigs.get(i + 3).setIdentifier(MONITORING_SOURCE_SUFFIX + "1");
    }
    save(cvConfigs);
    List<CVConfig> cvConfigsList = cvConfigService.listByMonitoringSources(accountId, orgIdentifier, projectIdentifier,
        Arrays.asList(MONITORING_SOURCE_SUFFIX + "0", MONITORING_SOURCE_SUFFIX + "1"));
    assertThat(cvConfigsList.size()).isEqualTo(6);
  }

  private void assertCommons(CVConfig actual, CVConfig expected) {
    assertThat(actual.getVerificationType()).isEqualTo(expected.getVerificationType());
    assertThat(actual.getAccountId()).isEqualTo(expected.getAccountId());
    assertThat(actual.getConnectorIdentifier()).isEqualTo(expected.getConnectorIdentifier());
    assertThat(actual.getServiceIdentifier()).isEqualTo(expected.getServiceIdentifier());
    assertThat(actual.getEnvIdentifier()).isEqualTo(expected.getEnvIdentifier());
    assertThat(actual.getProjectIdentifier()).isEqualTo(expected.getProjectIdentifier());
    assertThat(actual.getIdentifier()).isEqualTo(expected.getIdentifier());
    assertThat(actual.getCategory()).isEqualTo(expected.getCategory());
    assertThat(actual.getProductName()).isEqualTo(expected.getProductName());
    assertThat(actual.getType()).isEqualTo(expected.getType());
    assertThat(actual.getIdentifier()).isEqualTo(expected.getIdentifier());
    assertThat(actual.getMonitoringSourceName()).isEqualTo(expected.getMonitoringSourceName());
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

  private AppDynamicsCVConfig createAppDCVConfig() {
    AppDynamicsCVConfig appDynamicsCVConfig = new AppDynamicsCVConfig();
    fillCommon(appDynamicsCVConfig);
    appDynamicsCVConfig.setApplicationName("application-name");
    appDynamicsCVConfig.setTierName("tier-name");
    appDynamicsCVConfig.setMetricPack(MetricPack.builder().build());
    return appDynamicsCVConfig;
  }

  private void fillCommon(CVConfig cvConfig) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(connectorIdentifier);
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setIdentifier(groupId);
    cvConfig.setMonitoringSourceName(generateUuid());
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(productName);
    cvConfig.setIdentifier(monitoringSourceIdentifier);
    cvConfig.setMonitoringSourceName(monitoringSourceName);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testDoesAnyCVConfigExistsInProjectWhenNoCVConfigExists() {
    boolean doesAnyCVConfigExists =
        cvConfigService.doesAnyCVConfigExistsInProject(accountId, orgIdentifier, projectIdentifier);
    assertThat(doesAnyCVConfigExists).isFalse();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testDoesAnyCVConfigExistsInProjectWhenCVConfigExists() {
    List<CVConfig> cvConfigs = createCVConfigs(4);
    save(cvConfigs);
    boolean doesAnyCVConfigExists =
        cvConfigService.doesAnyCVConfigExistsInProject(accountId, orgIdentifier, projectIdentifier);
    assertThat(doesAnyCVConfigExists).isTrue();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testGetNumberOfServicesSetup() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    for (int i = 0; i < 3; i++) {
      cvConfigs.get(i).setServiceIdentifier("serviceIdentifier " + i);
    }
    cvConfigs.get(4).setServiceIdentifier("serviceIdentifier " + 0);
    save(cvConfigs);
    int numberOfServices = cvConfigService.getNumberOfServicesSetup(accountId, orgIdentifier, projectIdentifier);
    assertThat(numberOfServices).isEqualTo(4);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDeleteConfigsForProject() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    cvConfigs.get(0).setProjectIdentifier("newProject");
    save(cvConfigs);
    cvConfigService.deleteByProjectIdentifier(CVConfig.class, accountId, orgIdentifier, projectIdentifier);
    assertThat(cvConfigService.get(cvConfigs.get(0).getUuid())).isEqualTo(cvConfigs.get(0));
    for (int i = 1; i < 5; i++) {
      assertThat(cvConfigService.get(cvConfigs.get(i).getUuid())).isNull();
    }
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testDeleteConfigsForOrganisation() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    cvConfigs.get(0).setOrgIdentifier("newOrganisation");
    save(cvConfigs);
    cvConfigService.deleteByOrgIdentifier(CVConfig.class, accountId, orgIdentifier);
    assertThat(cvConfigService.get(cvConfigs.get(0).getUuid())).isEqualTo(cvConfigs.get(0));
    for (int i = 1; i < 5; i++) {
      assertThat(cvConfigService.get(cvConfigs.get(i).getUuid())).isNull();
    }
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testDeleteConfigsForAccount() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    cvConfigs.get(0).setAccountId("newAccount");
    save(cvConfigs);
    cvConfigService.deleteByAccountIdentifier(CVConfig.class, accountId);
    assertThat(cvConfigService.get(cvConfigs.get(0).getUuid())).isEqualTo(cvConfigs.get(0));
    for (int i = 1; i < 5; i++) {
      assertThat(cvConfigService.get(cvConfigs.get(i).getUuid())).isNull();
    }
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFindByConnectorIdentifier_projectScope() {
    List<CVConfig> cvConfigs = createCVConfigs(5);
    save(cvConfigs);
    List<CVConfig> result = cvConfigService.findByConnectorIdentifier(
        accountId, orgIdentifier, projectIdentifier, connectorIdentifier, Scope.PROJECT);
    Collections.sort(cvConfigs, Comparator.comparing(CVConfig::getUuid));
    Collections.sort(result, Comparator.comparing(CVConfig::getUuid));
    assertThat(result).isEqualTo(cvConfigs);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFindByConnectorIdentifier_accountScope() {
    List<CVConfig> projectScoped = createCVConfigs(5);
    save(projectScoped);
    String projectScopedConnectorIdentifier = connectorIdentifier;
    connectorIdentifier = "account.connectorId";
    List<CVConfig> cvConfigs = createCVConfigs(5);
    save(cvConfigs);
    List<CVConfig> result = cvConfigService.findByConnectorIdentifier(accountId, "", "", "connectorId", Scope.ACCOUNT);
    Collections.sort(cvConfigs, Comparator.comparing(CVConfig::getUuid));
    Collections.sort(result, Comparator.comparing(CVConfig::getUuid));
    assertThat(result).isEqualTo(cvConfigs);
    result = cvConfigService.findByConnectorIdentifier(
        accountId, orgIdentifier, projectIdentifier, projectScopedConnectorIdentifier, Scope.PROJECT);
    Collections.sort(projectScoped, Comparator.comparing(CVConfig::getUuid));
    Collections.sort(result, Comparator.comparing(CVConfig::getUuid));
    assertThat(result).isEqualTo(projectScoped);

    result =
        cvConfigService.findByConnectorIdentifier(accountId, orgIdentifier, projectIdentifier, "random", Scope.ACCOUNT);
    assertThat(result).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testFindByConnectorIdentifier_OrgIdentifierScope() {
    List<CVConfig> projectScoped = createCVConfigs(5);
    save(projectScoped);
    String projectScopedConnectorIdentifier = connectorIdentifier;
    connectorIdentifier = "org.connectorId";
    List<CVConfig> cvConfigs = createCVConfigs(5);
    save(cvConfigs);
    List<CVConfig> result =
        cvConfigService.findByConnectorIdentifier(accountId, orgIdentifier, "", "connectorId", Scope.ORG);
    Collections.sort(cvConfigs, Comparator.comparing(CVConfig::getUuid));
    Collections.sort(result, Comparator.comparing(CVConfig::getUuid));
    assertThat(result).isEqualTo(cvConfigs);
    result = cvConfigService.findByConnectorIdentifier(
        accountId, orgIdentifier, projectIdentifier, projectScopedConnectorIdentifier, Scope.PROJECT);
    Collections.sort(projectScoped, Comparator.comparing(CVConfig::getUuid));
    Collections.sort(result, Comparator.comparing(CVConfig::getUuid));
    assertThat(result).isEqualTo(projectScoped);

    result =
        cvConfigService.findByConnectorIdentifier(accountId, orgIdentifier, projectIdentifier, "random", Scope.ACCOUNT);
    assertThat(result).isEmpty();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testListByMonitoringSource() {
    String monSourceId = "monitoringSource1";
    CVConfig cvConfig = createCVConfig();
    cvConfig.setIdentifier(monSourceId);
    CVConfig updated = save(cvConfig);
    List<CVConfig> results = cvConfigService.listByMonitoringSources(
        accountId, orgIdentifier, projectIdentifier, "service", "env", Arrays.asList(monSourceId));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getUuid()).isEqualTo(updated.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testListByMonitoringSource_multiple() {
    String monSourceId = "monitoringSource1";
    CVConfig cvConfig = createCVConfig();
    cvConfig.setIdentifier(monSourceId);
    CVConfig updated = save(cvConfig);
    CVConfig cvConfig2 = createCVConfig();
    cvConfig2.setIdentifier(monSourceId + "2");
    CVConfig updated2 = save(cvConfig2);
    List<CVConfig> results = cvConfigService.listByMonitoringSources(
        accountId, orgIdentifier, projectIdentifier, "service", "env", Arrays.asList(monSourceId, monSourceId + "2"));
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getUuid()).isEqualTo(updated.getUuid());
    assertThat(results.get(1).getUuid()).isEqualTo(updated2.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testListByMonitoringSource_filterById() {
    String monSourceId = "monitoringSource1";
    CVConfig cvConfig = createCVConfig();
    cvConfig.setIdentifier(monSourceId);
    CVConfig updated = save(cvConfig);
    CVConfig cvConfig2 = createCVConfig();
    cvConfig2.setIdentifier(monSourceId + "2");
    CVConfig updated2 = save(cvConfig2);
    List<CVConfig> results = cvConfigService.listByMonitoringSources(
        accountId, orgIdentifier, projectIdentifier, "service", "env", Arrays.asList(monSourceId));
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getUuid()).isEqualTo(updated.getUuid());
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testListByMonitoringSource_null() {
    String monSourceId = "monitoringSource1";
    CVConfig cvConfig = createCVConfig();
    cvConfig.setIdentifier(monSourceId);
    CVConfig updated = save(cvConfig);
    CVConfig cvConfig2 = createCVConfig();
    cvConfig2.setIdentifier(monSourceId + "2");
    CVConfig updated2 = save(cvConfig2);
    List<CVConfig> results =
        cvConfigService.listByMonitoringSources(accountId, orgIdentifier, projectIdentifier, "service", "env", null);
    assertThat(results).hasSize(2);
    assertThat(results.get(0).getUuid()).isEqualTo(updated.getUuid());
    assertThat(results.get(1).getUuid()).isEqualTo(updated2.getUuid());
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testList_filteredWithIdentifiers() {
    String identifierOne = "identifierOne";
    String identifierTwo = "identifierTwo";
    String identifierThree = "identifierThree";
    List<String> healthSourceIds = Arrays.asList(identifierOne, identifierTwo);
    save(builderFactory.appDynamicsCVConfigBuilder()
             .identifier(identifierOne)
             .serviceIdentifier(serviceInstanceIdentifier)
             .envIdentifier(environmentIdentifier)
             .build());
    save(builderFactory.appDynamicsCVConfigBuilder()
             .identifier(identifierThree)
             .serviceIdentifier(serviceInstanceIdentifier)
             .envIdentifier(environmentIdentifier)
             .build());
    List<CVConfig> cvConfigs = cvConfigService.list(serviceEnvironmentParams, healthSourceIds);
    assertThat(cvConfigs).hasSize(1);
    assertThat(cvConfigs.get(0).getIdentifier()).isEqualTo(identifierOne);
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testList_fromServiceEnvironmentParams() {
    String identifierOne = "identifierOne";
    String identifierTwo = "identifierTwo";
    List<String> healthSourceIds = Arrays.asList(identifierOne, identifierTwo);
    save(builderFactory.appDynamicsCVConfigBuilder()
             .identifier(identifierOne)
             .serviceIdentifier(serviceInstanceIdentifier)
             .envIdentifier(environmentIdentifier)
             .build());
    save(builderFactory.appDynamicsCVConfigBuilder()
             .identifier(identifierTwo)
             .serviceIdentifier(serviceInstanceIdentifier)
             .envIdentifier(environmentIdentifier)
             .build());
    List<CVConfig> cvConfigs = cvConfigService.list(serviceEnvironmentParams);
    assertThat(cvConfigs).hasSize(2);
  }
}

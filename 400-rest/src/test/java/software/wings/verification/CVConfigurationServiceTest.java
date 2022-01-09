/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.verification;

import static io.harness.rule.OwnerRule.PRAVEEN;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.integration.IntegrationTestBase;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVConfigurationServiceTest extends IntegrationTestBase {
  @Inject CVConfigurationService cvConfigurationService;

  private String envId;
  private String serviceId;
  private String appId;
  private String connectorId;
  private String accountId;
  private String tierId;

  @Before
  public void setupTests() {
    accountId = generateUUID();
    envId = generateUUID();
    serviceId = generateUUID();
    appId = generateUUID();
    connectorId = generateUUID();
    tierId = "123456";
  }

  @Test(expected = WingsException.class)
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("Ignoring this test since it is failing in bazel and deprecated test do not runs")
  public void testSaveDuplicateName() {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration =
        AppDynamicsCVServiceConfiguration.builder().tierId(tierId).appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration);
    cvServiceConfiguration.setName("testName12");
    cvConfigurationService.saveConfiguration(accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration);

    AppDynamicsCVServiceConfiguration cvServiceConfiguration2 =
        AppDynamicsCVServiceConfiguration.builder().tierId(tierId).appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration2);
    cvServiceConfiguration2.setName("testName12");
    cvConfigurationService.saveConfiguration(accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration2);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("Ignoring this test since it is failing in bazel and deprecated test do not runs")
  public void testUpdateGoodCase() {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration =
        AppDynamicsCVServiceConfiguration.builder().tierId(tierId).appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration);
    cvServiceConfiguration.setName("testName12");
    String uuid =
        cvConfigurationService.saveConfiguration(accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration);

    AppDynamicsCVServiceConfiguration cvServiceConfiguration2 =
        AppDynamicsCVServiceConfiguration.builder().tierId(tierId).appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration2);
    cvServiceConfiguration2.setName("testName123");
    String uuid2 =
        cvConfigurationService.saveConfiguration(accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration2);

    cvServiceConfiguration2.setName("test12345");
    String updated = cvConfigurationService.updateConfiguration(
        accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration2, uuid2);

    assertThat(updated).isEqualTo(uuid2, "Updated UUID should be same");
  }

  @Test(expected = WingsException.class)
  @Owner(developers = PRAVEEN)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("Ignoring this test since it is failing in bazel and deprecated test do not runs")
  public void testUpdateDuplicateName() {
    AppDynamicsCVServiceConfiguration cvServiceConfiguration =
        AppDynamicsCVServiceConfiguration.builder().tierId(tierId).appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration);
    cvServiceConfiguration.setName("testName12");
    String uuid =
        cvConfigurationService.saveConfiguration(accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration);

    AppDynamicsCVServiceConfiguration cvServiceConfiguration2 =
        AppDynamicsCVServiceConfiguration.builder().tierId(tierId).appDynamicsApplicationId("1234").build();
    setBasicInfo(cvServiceConfiguration2);
    cvServiceConfiguration2.setName("testName123");
    String uuid2 =
        cvConfigurationService.saveConfiguration(accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration2);

    cvServiceConfiguration.setUuid(uuid2);
    String updated = cvConfigurationService.updateConfiguration(
        accountId, appId, StateType.APP_DYNAMICS, cvServiceConfiguration, uuid2);
  }

  private void setBasicInfo(AppDynamicsCVServiceConfiguration cvServiceConfiguration) {
    cvServiceConfiguration.setStateType(StateType.APP_DYNAMICS);
    cvServiceConfiguration.setAccountId(accountId);
    cvServiceConfiguration.setServiceId(serviceId);
    cvServiceConfiguration.setConnectorId(connectorId);
    cvServiceConfiguration.setEnvId(envId);
    cvServiceConfiguration.setAppId(appId);
    cvServiceConfiguration.setEnabled24x7(true);
    cvServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.LOW);
    cvServiceConfiguration.setName("TestAppDConfig");
  }
}

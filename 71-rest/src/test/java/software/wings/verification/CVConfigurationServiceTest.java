package software.wings.verification;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.StateType;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;

public class CVConfigurationServiceTest extends BaseIntegrationTest {
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
  @Category(IntegrationTests.class)
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
  @Category(IntegrationTests.class)
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

    assertEquals("Updated UUID should be same", uuid2, updated);
  }

  @Test(expected = WingsException.class)
  @Category(IntegrationTests.class)
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

package software.wings.integration.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.sm.StateType.APP_DYNAMICS;
import static software.wings.sm.StateType.NEW_RELIC;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.AppService;
import software.wings.verification.CVConfiguration;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */
public class CVConfigurationIntegrationTest extends BaseIntegrationTest {
  private String appId, envId, serviceId, appDynamicsApplicationId;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  private NewRelicCVServiceConfiguration newRelicCVServiceConfiguration;
  private AppDynamicsCVServiceConfiguration appDynamicsCVServiceConfiguration;

  @Before
  public void setUp() {
    loginAdminUser();
    appId = appService.save(anApplication().withName(generateUuid()).withAccountId(accountId).build()).getUuid();
    envId = generateUuid();
    serviceId = generateUuid();
    appDynamicsApplicationId = generateUuid();

    createNewRelicConfig(true);
    createAppDynamicsConfig();
  }

  private void createNewRelicConfig(boolean enabled24x7) {
    String newRelicApplicationId = generateUuid();
    String newRelicServerSettingId = generateUuid();

    newRelicCVServiceConfiguration = new NewRelicCVServiceConfiguration();
    newRelicCVServiceConfiguration.setEnvId(envId);
    newRelicCVServiceConfiguration.setServiceId(serviceId);
    newRelicCVServiceConfiguration.setEnabled24x7(enabled24x7);
    newRelicCVServiceConfiguration.setApplicationId(newRelicApplicationId);
    newRelicCVServiceConfiguration.setConnectorId(newRelicServerSettingId);
    newRelicCVServiceConfiguration.setMetrics(Collections.singletonList(generateUuid()));
  }

  private void createAppDynamicsConfig() {
    appDynamicsCVServiceConfiguration = new AppDynamicsCVServiceConfiguration();
    appDynamicsCVServiceConfiguration.setAppId(appId);
    appDynamicsCVServiceConfiguration.setEnvId(envId);
    appDynamicsCVServiceConfiguration.setServiceId(serviceId);
    appDynamicsCVServiceConfiguration.setEnabled24x7(true);
    appDynamicsCVServiceConfiguration.setAppDynamicsApplicationId(appDynamicsApplicationId);
    appDynamicsCVServiceConfiguration.setTierId(generateUuid());
    appDynamicsCVServiceConfiguration.setConnectorId(generateUuid());
    appDynamicsCVServiceConfiguration.setStateType(APP_DYNAMICS);
  }

  @Test
  public void testSaveConfiguration() {
    String id = wingsPersistence.save(newRelicCVServiceConfiguration);
    NewRelicCVServiceConfiguration obj = wingsPersistence.get(NewRelicCVServiceConfiguration.class, id);
    assertEquals(id, obj.getUuid());
  }

  @Test
  public <T extends CVConfiguration> void testNewRelicConfiguration() {
    String url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + NEW_RELIC;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();

    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(NEW_RELIC, fetchedObject.getStateType());

    url = API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId;
    target = client.target(url);

    RestResponse<List<T>> allConfigResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<T>>>() {});
    List<T> allConifgs = allConfigResponse.getResource();

    assertEquals(1, allConifgs.size());
    fetchedObject = allConifgs.get(0);

    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(accountId, fetchedObject.getAccountId());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
    assertEquals(NEW_RELIC, fetchedObject.getStateType());

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId
        + "&stateType=" + NEW_RELIC + "&serviceConfigurationId=" + savedObjectUuid;
    target = client.target(url);
    newRelicCVServiceConfiguration.setEnabled24x7(false);
    newRelicCVServiceConfiguration.setMetrics(Collections.singletonList("requestsPerMinute"));
    getRequestBuilderWithAuthHeader(target).put(
        entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    getRequestResponse = getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    fetchedObject = getRequestResponse.getResource();
    assertFalse(fetchedObject.isEnabled24x7());

    String delete_url =
        API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    RestResponse<Boolean> response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(true, response.getResource());

    delete_url =
        API_BASE + "/cv-configuration/" + UUID.randomUUID().toString() + "?accountId=" + accountId + "&appId=" + appId;
    target = client.target(delete_url);
    response = getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse>() {});
    assertEquals(false, response.getResource());
  }

  @Test
  public <T extends CVConfiguration> void testAppDynamicsConfiguration() {
    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + APP_DYNAMICS;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(appDynamicsCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv-configuration/" + savedObjectUuid + "?accountId=" + accountId
        + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();
    if (fetchedObject instanceof AppDynamicsCVServiceConfiguration) {
      AppDynamicsCVServiceConfiguration obj = (AppDynamicsCVServiceConfiguration) fetchedObject;
      assertEquals(savedObjectUuid, obj.getUuid());
      assertEquals(accountId, obj.getAccountId());
      assertEquals(appId, obj.getAppId());
      assertEquals(envId, obj.getEnvId());
      assertEquals(serviceId, obj.getServiceId());
      assertEquals(APP_DYNAMICS, obj.getStateType());
      assertEquals(appDynamicsApplicationId, obj.getAppDynamicsApplicationId());
    }
  }
}

package software.wings.integration.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.AppService;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */
public class CVConfigurationIntegrationTest extends BaseIntegrationTest {
  private String appId, envId, serviceId;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private AppService appService;

  private NewRelicCVServiceConfiguration newRelicCVServiceConfiguration;

  @Before
  public void setUp() {
    loginAdminUser();
    appId = appService.save(anApplication().withName(generateUuid()).withAccountId(accountId).build()).getUuid();
    envId = generateUuid();
    serviceId = generateUuid();
    boolean enabled24x7 = true;

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

  @Test
  public void testSaveConfiguration() {
    String id = wingsPersistence.save(newRelicCVServiceConfiguration);
    NewRelicCVServiceConfiguration obj = wingsPersistence.get(NewRelicCVServiceConfiguration.class, id);
    assertEquals(id, obj.getUuid());
  }

  @Test
  public <T extends CVConfiguration> void testNewRelicConfiguration() {
    String url =
        API_BASE + "/cv-configuration?accountId=" + accountId + "&appId=" + appId + "&stateType=" + StateType.NEW_RELIC;
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
    assertEquals(StateType.NEW_RELIC, fetchedObject.getStateType());

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
    assertEquals(StateType.NEW_RELIC, fetchedObject.getStateType());
  }
}

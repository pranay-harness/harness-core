package software.wings.integration.verification;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.beans.RestResponse;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.Collections;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author Vaibhav Tulsyan
 * 05/Oct/2018
 */
public class CVConfigurationIntegrationTest extends BaseIntegrationTest {
  private String appId, envId, serviceId;

  @Inject WingsPersistence wingsPersistence;

  private NewRelicCVServiceConfiguration newRelicCVServiceConfiguration;

  @Before
  public void setUp() {
    appId = UUID.randomUUID().toString();
    envId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    boolean enabled24x7 = true;

    String newRelicApplicationId = UUID.randomUUID().toString();
    String newRelicServerSettingId = UUID.randomUUID().toString();

    newRelicCVServiceConfiguration = NewRelicCVServiceConfiguration.builder()
                                         .appId(appId)
                                         .envId(envId)
                                         .serviceId(serviceId)
                                         .enabled24x7(enabled24x7)
                                         .applicationId(newRelicApplicationId)
                                         .newRelicServerSettingId(newRelicServerSettingId)
                                         .metrics(Collections.singletonList("W4nn78SfBdMetric"))
                                         .build();

    loginAdminUser();
  }

  @Test
  public void testSaveConfiguration() {
    String id = wingsPersistence.save(newRelicCVServiceConfiguration);
    NewRelicCVServiceConfiguration obj = wingsPersistence.get(NewRelicCVServiceConfiguration.class, id);
    assertEquals(id, obj.getUuid());
  }

  @Test
  public <T extends CVConfiguration> void testNewRelicConfiguration() {
    String url = API_BASE + "/cv/configure?accountId=" + accountId + "&stateType=" + StateType.NEW_RELIC;
    logger.info("POST " + url);
    WebTarget target = client.target(url);
    RestResponse<String> restResponse = getRequestBuilderWithAuthHeader(target).post(
        entity(newRelicCVServiceConfiguration, APPLICATION_JSON), new GenericType<RestResponse<String>>() {});
    String savedObjectUuid = restResponse.getResource();

    url = API_BASE + "/cv/get-configuration?accountId=" + accountId + "&serviceConfigurationId=" + savedObjectUuid;

    target = client.target(url);
    RestResponse<T> getRequestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<T>>() {});
    T fetchedObject = getRequestResponse.getResource();

    assertEquals(savedObjectUuid, fetchedObject.getUuid());
    assertEquals(appId, fetchedObject.getAppId());
    assertEquals(envId, fetchedObject.getEnvId());
    assertEquals(serviceId, fetchedObject.getServiceId());
  }
}

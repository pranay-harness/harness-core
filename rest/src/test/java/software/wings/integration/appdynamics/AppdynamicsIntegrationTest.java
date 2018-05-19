package software.wings.integration.appdynamics;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.rule.RepeatRule.Repeat;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;

import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/11/17.
 */
public class AppdynamicsIntegrationTest extends BaseIntegrationTest {
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;

  private String appdynamicsSettingId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
    SettingAttribute appdSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("AppDynamics" + System.currentTimeMillis())
            .withAccountId(accountId)
            .withValue(AppDynamicsConfig.builder()
                           .accountId(accountId)
                           .controllerUrl("https://harness-test.saas.appdynamics.com/controller")
                           .username("raghu@harness.io")
                           .accountname("harness-test")
                           .password("(idlk2e9idcs@ej".toCharArray())
                           .build())
            .build();
    appdynamicsSettingId = wingsPersistence.saveAndGet(SettingAttribute.class, appdSettingAttribute).getUuid();
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void testGetAllApplications() throws Exception {
    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    assertEquals(0, restResponse.getResponseMessages().size());
    assertFalse(restResponse.getResource().isEmpty());

    for (NewRelicApplication app : restResponse.getResource()) {
      assertTrue(app.getId() > 0);
      assertFalse(isBlank(app.getName()));
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void testGetAllTiers() throws Exception {
    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    for (NewRelicApplication application : restResponse.getResource()) {
      WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettingId
          + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId());
      RestResponse<List<AppdynamicsTier>> tierRestResponse =
          getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
      assertFalse(tierRestResponse.getResource().isEmpty());

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        assertTrue(tier.getId() > 0);
        assertFalse(isBlank(tier.getName()));
        assertFalse(isBlank(tier.getType()));
        assertFalse(isBlank(tier.getAgentType()));
        assertFalse(tier.getName().isEmpty());
      }
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  @Ignore
  public void testGetAllTierBTMetrics() throws Exception {
    SettingAttribute appdSettingAttribute = settingsService.get(appdynamicsSettingId);
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) appdSettingAttribute.getValue();

    // get all applications
    WebTarget target = client.target(
        API_BASE + "/appdynamics/applications?settingId=" + appdynamicsSettingId + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    for (NewRelicApplication application : restResponse.getResource()) {
      WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettingId
          + "&accountId=" + accountId + "&appdynamicsAppId=" + application.getId());
      RestResponse<List<AppdynamicsTier>> tierRestResponse =
          getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
      assertFalse(tierRestResponse.getResource().isEmpty());

      for (AppdynamicsTier tier : tierRestResponse.getResource()) {
        List<AppdynamicsMetric> btMetrics = appdynamicsDelegateService.getTierBTMetrics(appDynamicsConfig,
            application.getId(), tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null));

        assertFalse(btMetrics.isEmpty());

        for (AppdynamicsMetric btMetric : btMetrics) {
          assertFalse(isBlank(btMetric.getName()));
          assertFalse("failed for " + btMetric.getName(), btMetric.getChildMetrices().isEmpty());

          for (AppdynamicsMetric leafMetric : btMetric.getChildMetrices()) {
            assertFalse(isBlank(leafMetric.getName()));
            assertEquals("failed for " + btMetric.getName() + "|" + leafMetric.getName(), 0,
                leafMetric.getChildMetrices().size());
          }
        }
      }
    }
  }
}

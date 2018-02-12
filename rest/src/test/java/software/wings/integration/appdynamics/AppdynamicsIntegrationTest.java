package software.wings.integration.appdynamics;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.utils.JsonUtils;

import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/11/17.
 */
public class AppdynamicsIntegrationTest extends BaseIntegrationTest {
  final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;
  @Inject private SecretManager secretManager;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(asList(SettingAttribute.class));
    SettingAttribute appdSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("AppDynamics")
            .withAccountId(ACCOUNT_ID)
            .withValue(AppDynamicsConfig.builder()
                           .controllerUrl("https://wings251.saas.appdynamics.com/controller")
                           .username("appd-user")
                           .accountname("wings251")
                           .password("5PdEYf9H".toCharArray())
                           .accountId(ACCOUNT_ID)
                           .build())
            .build();
    wingsPersistence.saveAndGet(SettingAttribute.class, appdSettingAttribute);
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void testGetAllApplications() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
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
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    long appId = 0;

    for (NewRelicApplication application : restResponse.getResource()) {
      if (application.getName().equalsIgnoreCase("MyApp")) {
        appId = application.getId();
        break;
      }
    }

    assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    assertFalse(tierRestResponse.getResource().isEmpty());

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      assertTrue(tier.getId() > 0);
      assertFalse(isBlank(tier.getName()));
      assertFalse(isBlank(tier.getType()));
      assertFalse(isBlank(tier.getAgentType()));
      assertEquals(tier.getName(), "MyTier");
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void testGetAllTierBTMetrics() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    assertEquals(1, appdynamicsSettings.size());
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) appdynamicsSettings.get(0).getValue();

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    long appId = 0;

    for (NewRelicApplication application : restResponse.getResource()) {
      if (application.getName().equalsIgnoreCase("MyApp")) {
        appId = application.getId();
        break;
      }
    }

    assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    assertFalse(tierRestResponse.getResource().isEmpty());

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      List<AppdynamicsMetric> btMetrics = appdynamicsDelegateService.getTierBTMetrics(
          appDynamicsConfig, appId, tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null));

      assertFalse(btMetrics.isEmpty());

      for (AppdynamicsMetric btMetric : btMetrics) {
        assertFalse(isBlank(btMetric.getName()));
        assertFalse("failed for " + btMetric.getName(), btMetric.getChildMetrices().isEmpty());

        for (AppdynamicsMetric leafMetric : btMetric.getChildMetrices()) {
          assertFalse(isBlank(leafMetric.getName()));
          assertEquals(
              "failed for " + btMetric.getName() + "|" + leafMetric.getName(), 0, leafMetric.getChildMetrices().size());
        }
      }
    }
  }

  @Test
  @Ignore
  public void testGetBTMetricData() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    assertEquals(1, appdynamicsSettings.size());
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) appdynamicsSettings.get(0).getValue();

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    long appId = 0;

    for (NewRelicApplication application : restResponse.getResource()) {
      if (application.getName().equalsIgnoreCase("MyApp")) {
        appId = application.getId();
        break;
      }
    }

    assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    assertFalse(tierRestResponse.getResource().isEmpty());

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      List<AppdynamicsMetric> btMetrics = appdynamicsDelegateService.getTierBTMetrics(
          appDynamicsConfig, appId, tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null));
      assertFalse(btMetrics.isEmpty());

      List<AppdynamicsNode> nodes = appdynamicsDelegateService.getNodes(
          appDynamicsConfig, appId, tier.getId(), secretManager.getEncryptionDetails(appDynamicsConfig, null, null));

      for (AppdynamicsMetric btMetric : btMetrics) {
        assertFalse(isBlank(btMetric.getName()));
        assertFalse("failed for " + btMetric.getName(), btMetric.getChildMetrices().isEmpty());

        List<AppdynamicsMetricData> tierBTMetricData =
            appdynamicsDelegateService.getTierBTMetricData(appDynamicsConfig, appId, tier.getId(), btMetric.getName(),
                nodes.get(0).getName(), 5, secretManager.getEncryptionDetails(appDynamicsConfig, null, null));
        assertNotNull(tierBTMetricData);
        System.out.println(JsonUtils.asJson(tierBTMetricData.size()));
      }
    }
  }
}

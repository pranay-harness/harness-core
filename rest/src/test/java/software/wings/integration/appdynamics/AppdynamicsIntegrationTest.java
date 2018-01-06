package software.wings.integration.appdynamics;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.RepeatRule.Repeat;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.utils.JsonUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/11/17.
 */
public class AppdynamicsIntegrationTest extends BaseIntegrationTest {
  final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";

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
    Assert.assertTrue(restResponse.getResource().size() > 0);

    for (NewRelicApplication app : restResponse.getResource()) {
      Assert.assertTrue(app.getId() > 0);
      Assert.assertFalse(StringUtils.isBlank(app.getName()));
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

    Assert.assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    Assert.assertTrue(tierRestResponse.getResource().size() > 0);

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      Assert.assertTrue(tier.getId() > 0);
      Assert.assertFalse(StringUtils.isBlank(tier.getName()));
      Assert.assertFalse(StringUtils.isBlank(tier.getType()));
      Assert.assertFalse(StringUtils.isBlank(tier.getAgentType()));
      assertEquals(tier.getName(), "MyTier");
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void testGetAllBusinessTransactions() throws Exception {
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

    Assert.assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/business-transactions?settingId="
        + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsBusinessTransaction>> btRestResponse = getRequestBuilderWithAuthHeader(btTarget).get(
        new GenericType<RestResponse<List<AppdynamicsBusinessTransaction>>>() {});
    Assert.assertTrue(btRestResponse.getResource().size() > 0);

    for (AppdynamicsBusinessTransaction bt : btRestResponse.getResource()) {
      Assert.assertTrue(bt.getId() > 0);
      Assert.assertTrue(bt.getTierId() > 0);
      Assert.assertFalse(StringUtils.isBlank(bt.getName()));
      Assert.assertFalse(StringUtils.isBlank(bt.getEntryPointType()));
      Assert.assertFalse(StringUtils.isBlank(bt.getInternalName()));
      Assert.assertFalse(StringUtils.isBlank(bt.getTierName()));
      Assert.assertFalse(StringUtils.isBlank(bt.getInternalName()));
    }
  }

  @Test
  @Repeat(times = 5, successes = 1)
  public void testGetAllTierBTMetrics() throws Exception {
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

    Assert.assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    Assert.assertTrue(tierRestResponse.getResource().size() > 0);

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      WebTarget btMetricsTarget =
          client.target(API_BASE + "/appdynamics/tier-bt-metrics?settingId=" + appdynamicsSettings.get(0).getUuid()
              + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId + "&tierId=" + tier.getId());
      RestResponse<List<AppdynamicsMetric>> tierBTMResponse =
          getRequestBuilderWithAuthHeader(btMetricsTarget)
              .get(new GenericType<RestResponse<List<AppdynamicsMetric>>>() {});

      List<AppdynamicsMetric> btMetrics = tierBTMResponse.getResource();
      Assert.assertTrue(btMetrics.size() > 0);

      for (AppdynamicsMetric btMetric : btMetrics) {
        Assert.assertFalse(StringUtils.isBlank(btMetric.getName()));
        Assert.assertTrue("failed for " + btMetric.getName(), btMetric.getChildMetrices().size() > 0);

        for (AppdynamicsMetric leafMetric : btMetric.getChildMetrices()) {
          Assert.assertFalse(StringUtils.isBlank(leafMetric.getName()));
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

    Assert.assertTrue("could not find MyApp application in appdynamics", appId > 0);
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + appdynamicsSettings.get(0).getUuid()
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    Assert.assertTrue(tierRestResponse.getResource().size() > 0);

    for (AppdynamicsTier tier : tierRestResponse.getResource()) {
      WebTarget btMetricsTarget =
          client.target(API_BASE + "/appdynamics/tier-bt-metrics?settingId=" + appdynamicsSettings.get(0).getUuid()
              + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId + "&tierId=" + tier.getId());
      RestResponse<List<AppdynamicsMetric>> tierBTMResponse =
          getRequestBuilderWithAuthHeader(btMetricsTarget)
              .get(new GenericType<RestResponse<List<AppdynamicsMetric>>>() {});

      List<AppdynamicsMetric> btMetrics = tierBTMResponse.getResource();
      Assert.assertTrue(btMetrics.size() > 0);

      for (AppdynamicsMetric btMetric : btMetrics) {
        Assert.assertFalse(StringUtils.isBlank(btMetric.getName()));
        Assert.assertTrue("failed for " + btMetric.getName(), btMetric.getChildMetrices().size() > 0);

        final String url = API_BASE + "/appdynamics/get-metric-data?settingId=" + appdynamicsSettings.get(0).getUuid()
            + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId + "&tierId=" + tier.getId() + "&btName="
            + btMetric.getName() + "&startTime=" + (System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5))
            + "&endTime=" + System.currentTimeMillis();
        WebTarget btMetricsDataTarget = client.target(url.replaceAll(" ", "%20"));
        RestResponse<List<AppdynamicsMetricData>> tierBTMDataResponse =
            getRequestBuilderWithAuthHeader(btMetricsDataTarget)
                .get(new GenericType<RestResponse<List<AppdynamicsMetricData>>>() {});
        System.out.println(JsonUtils.asJson(tierBTMDataResponse.getResource()));
      }
    }
  }
}

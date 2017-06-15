package software.wings.integration.appdynamics;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import software.wings.api.AppDynamicsExecutionData;
import software.wings.beans.AppDynamicsConfig.Builder;
import software.wings.beans.Application;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.integration.BaseIntegrationTest;
import software.wings.metrics.MetricSummary;
import software.wings.metrics.RiskLevel;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.service.impl.appdynamics.AppdynamicsApplication;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsMetricData;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataRecord;
import software.wings.service.impl.appdynamics.AppdynamicsMetricDataValue;
import software.wings.service.impl.appdynamics.AppdynamicsNode;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 5/11/17.
 */
public class AppdynamicsIntegrationTest extends BaseIntegrationTest {
  @Inject private AppdynamicsService appdynamicsService;

  final long METRIC_ID = 4L;
  final String METRIC_NAME = AppdynamicsConstants.RESPONSE_TIME_95;
  final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(SettingAttribute.class));
    SettingAttribute appdSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("AppDynamics")
            .withAccountId(ACCOUNT_ID)
            .withValue(Builder.anAppDynamicsConfig()
                           .withControllerUrl("https://wings251.saas.appdynamics.com/controller")
                           .withUsername("appd-user")
                           .withAccountname("wings251")
                           .withPassword("5PdEYf9H".toCharArray())
                           .withAccountId(ACCOUNT_ID)
                           .build())
            .build();
    wingsPersistence.saveAndGet(SettingAttribute.class, appdSettingAttribute);
  }

  @Test
  public void testGetAllApplications() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertTrue(restResponse.getResource().size() > 0);

    for (AppdynamicsApplication app : restResponse.getResource()) {
      Assert.assertTrue(app.getId() > 0);
      Assert.assertFalse(StringUtils.isBlank(app.getName()));
    }
  }

  @Test
  public void testGetAllTiers() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
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
      Assert.assertTrue(tier.getNumberOfNodes() > 0);
    }
  }

  @Test
  public void testGetAllNodes() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
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
      Assert.assertTrue(tier.getNumberOfNodes() > 0);

      WebTarget nodeTarget =
          client.target(API_BASE + "/appdynamics/nodes?settingId=" + appdynamicsSettings.get(0).getUuid()
              + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId + "&tierId=" + tier.getId());

      RestResponse<List<AppdynamicsNode>> nodeRestResponse =
          getRequestBuilderWithAuthHeader(nodeTarget).get(new GenericType<RestResponse<List<AppdynamicsNode>>>() {});

      Assert.assertTrue(nodeRestResponse.getResource().size() > 0);
      for (AppdynamicsNode node : nodeRestResponse.getResource()) {
        Assert.assertTrue(node.getId() > 0);
        Assert.assertFalse(StringUtils.isBlank(node.getName()));
        Assert.assertFalse(StringUtils.isBlank(node.getType()));
        Assert.assertTrue(node.getTierId() > 0);
        Assert.assertFalse(StringUtils.isBlank(node.getTierName()));
        Assert.assertTrue(node.getMachineId() > 0);
        Assert.assertFalse(StringUtils.isBlank(node.getMachineName()));
        Assert.assertFalse(StringUtils.isBlank(node.getMachineOSType()));
        Assert.assertFalse(StringUtils.isBlank(node.getAppAgentVersion()));
        Assert.assertFalse(StringUtils.isBlank(node.getAgentType()));
        Assert.assertTrue(node.getIpAddresses().size() > 0);

        Assert.assertTrue(node.getIpAddresses().containsKey("ipAddresses"));
        Assert.assertTrue(node.getIpAddresses().get("ipAddresses").size() > 0);
      }
    }
  }

  @Test
  public void testGetAllBusinessTransactions() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
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
  public void testGetAllTierBTMetrics() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
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
          Assert.assertEquals(
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
    Assert.assertEquals(1, appdynamicsSettings.size());

    // get all applications
    WebTarget target = client.target(API_BASE
        + "/appdynamics/applications?settingId=" + appdynamicsSettings.get(0).getUuid() + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<AppdynamicsApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});

    long appId = 0;

    for (AppdynamicsApplication application : restResponse.getResource()) {
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

  @Test
  @Ignore
  public void testAppdynamicsPersistence() throws Exception {
    final List<SettingAttribute> appdynamicsSettings =
        settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, "APP_DYNAMICS");
    Assert.assertEquals(1, appdynamicsSettings.size());
    String settingId = appdynamicsSettings.get(0).getUuid();
    WebTarget appTarget =
        client.target(API_BASE + "/appdynamics/applications?settingId=" + settingId + "&accountId=" + ACCOUNT_ID);
    RestResponse<List<AppdynamicsApplication>> appRestResponse = getRequestBuilderWithAuthHeader(appTarget).get(
        new GenericType<RestResponse<List<AppdynamicsApplication>>>() {});
    long appId = 0;
    for (AppdynamicsApplication application : appRestResponse.getResource()) {
      if (application.getName().equalsIgnoreCase("MyApp")) {
        appId = application.getId();
        break;
      }
    }
    WebTarget btTarget = client.target(API_BASE + "/appdynamics/tiers?settingId=" + settingId
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId);
    RestResponse<List<AppdynamicsTier>> tierRestResponse =
        getRequestBuilderWithAuthHeader(btTarget).get(new GenericType<RestResponse<List<AppdynamicsTier>>>() {});
    AppdynamicsTier tier = tierRestResponse.getResource().get(0);
    WebTarget btMetricsTarget = client.target(API_BASE + "/appdynamics/tier-bt-metrics?settingId=" + settingId
        + "&accountId=" + ACCOUNT_ID + "&appdynamicsAppId=" + appId + "&tierId=" + tier.getId());
    RestResponse<List<AppdynamicsMetric>> tierBTMResponse =
        getRequestBuilderWithAuthHeader(btMetricsTarget).get(new GenericType<RestResponse<List<AppdynamicsMetric>>>() {
        });
    String btName = tierBTMResponse.getResource().get(0).getName();

    // delete stale data from previous runs
    Query<AppdynamicsMetricDataRecord> query = wingsPersistence.createQuery(AppdynamicsMetricDataRecord.class);
    query.filter("accountId = ", ACCOUNT_ID)
        .filter("appdAppId = ", appId)
        .filter("metricId", METRIC_ID)
        .filter("tierId", tier.getId());
    boolean success = wingsPersistence.delete(query);
    assert (success);

    final AppdynamicsMetricDataValue METRIC_VALUE_1 = AppdynamicsMetricDataValue.Builder.anAppdynamicsMetricDataValue()
                                                          .withStartTimeInMillis(1495432894010L)
                                                          .withValue(100L)
                                                          .withMin(5L)
                                                          .withMax(200L)
                                                          .withCurrent(50L)
                                                          .withSum(910L)
                                                          .withCount(8L)
                                                          .withStandardDeviation(0.5d)
                                                          .withOccurrences(3)
                                                          .withUseRange(true)
                                                          .build();
    final AppdynamicsMetricDataValue METRIC_VALUE_2 = AppdynamicsMetricDataValue.Builder.anAppdynamicsMetricDataValue()
                                                          .withStartTimeInMillis(1495432954010L)
                                                          .withValue(200L)
                                                          .withMin(10L)
                                                          .withMax(400L)
                                                          .withCurrent(100L)
                                                          .withSum(1800L)
                                                          .withCount(16L)
                                                          .withStandardDeviation(0.25d)
                                                          .withOccurrences(6)
                                                          .withUseRange(true)
                                                          .build();
    final AppdynamicsMetricDataValue METRIC_VALUE_3 = AppdynamicsMetricDataValue.Builder.anAppdynamicsMetricDataValue()
                                                          .withStartTimeInMillis(1495433014010L)
                                                          .withValue(300L)
                                                          .withMin(15L)
                                                          .withMax(600L)
                                                          .withCurrent(150L)
                                                          .withSum(2700L)
                                                          .withCount(24L)
                                                          .withStandardDeviation(0.125d)
                                                          .withOccurrences(9)
                                                          .withUseRange(true)
                                                          .build();
    final AppdynamicsMetricData METRIC_DATA_ALPHA_1 =
        AppdynamicsMetricData.Builder.anAppdynamicsMetricsData()
            .withMetricName("BTM|BTs|BT:132632|Component:42159|" + METRIC_NAME)
            .withMetricId(METRIC_ID)
            .withMetricPath("Business Transaction Performance|Business Transactions|test-tier|" + btName
                + "|Individual Nodes|alpha|" + METRIC_NAME)
            .withFrequency("ONE_MIN")
            .withMetricValues(new AppdynamicsMetricDataValue[] {METRIC_VALUE_1, METRIC_VALUE_2})
            .build();
    final AppdynamicsMetricData METRIC_DATA_ALPHA_2 =
        AppdynamicsMetricData.Builder.anAppdynamicsMetricsData()
            .withMetricName("BTM|BTs|BT:132632|Component:42159|" + METRIC_NAME)
            .withMetricId(METRIC_ID)
            .withMetricPath("Business Transaction Performance|Business Transactions|test-tier|" + btName
                + "|Individual Nodes|alpha|" + METRIC_NAME)
            .withFrequency("ONE_MIN")
            .withMetricValues(new AppdynamicsMetricDataValue[] {METRIC_VALUE_2, METRIC_VALUE_3})
            .build();
    final AppdynamicsMetricData METRIC_DATA_BETA_1 =
        AppdynamicsMetricData.Builder.anAppdynamicsMetricsData()
            .withMetricName("BTM|BTs|BT:132632|Component:42159|" + METRIC_NAME)
            .withMetricId(METRIC_ID)
            .withMetricPath("Business Transaction Performance|Business Transactions|test-tier|" + btName
                + "|Individual Nodes|beta|" + METRIC_NAME)
            .withFrequency("ONE_MIN")
            .withMetricValues(new AppdynamicsMetricDataValue[] {METRIC_VALUE_1, METRIC_VALUE_2})
            .build();
    final AppdynamicsMetricData METRIC_DATA_BETA_2 =
        AppdynamicsMetricData.Builder.anAppdynamicsMetricsData()
            .withMetricName("BTM|BTs|BT:132632|Component:42159|" + METRIC_NAME)
            .withMetricId(METRIC_ID)
            .withMetricPath("Business Transaction Performance|Business Transactions|test-tier|" + btName
                + "|Individual Nodes|beta|" + METRIC_NAME)
            .withFrequency("ONE_MIN")
            .withMetricValues(new AppdynamicsMetricDataValue[] {METRIC_VALUE_2, METRIC_VALUE_3})
            .build();
    final List<AppdynamicsMetricDataRecord> METRIC_DATA_RECORDS_ALPHA_1 =
        AppdynamicsMetricDataRecord.generateDataRecords(ACCOUNT_ID, appId, tier.getId(), METRIC_DATA_ALPHA_1);

    // insert metric values 1 and 2 using the REST interface
    WebTarget target = client.target(API_BASE + "/appdynamics/save-metrics?accountId=" + ACCOUNT_ID
        + "&appdynamicsAppId=" + appId + "&tierId=" + tier.getId());
    RestResponse<Boolean> restResponse = getRequestBuilderWithAuthHeader(target).post(
        Entity.entity(Arrays.asList(METRIC_DATA_ALPHA_1), APPLICATION_JSON),
        new GenericType<RestResponse<Boolean>>() {});
    assert (restResponse.getResource());

    // query
    PageRequest.Builder requestBuilder = aPageRequest()
                                             .addFilter("accountId", Operator.EQ, ACCOUNT_ID)
                                             .addFilter("appdAppId", Operator.EQ, appId)
                                             .addFilter("metricId", Operator.EQ, METRIC_ID)
                                             .addFilter("tierId", Operator.EQ, tier.getId())
                                             .addOrder("startTime", OrderType.ASC);
    PageResponse<AppdynamicsMetricDataRecord> response =
        wingsPersistence.query(AppdynamicsMetricDataRecord.class, requestBuilder.build());
    List<AppdynamicsMetricDataRecord> result = response.getResponse();
    Assert.assertEquals("result not correct length", 2, result.size());
    Assert.assertEquals("record 0 not equal", METRIC_DATA_RECORDS_ALPHA_1.get(0), result.get(0));
    Assert.assertEquals("record 1 not equal", METRIC_DATA_RECORDS_ALPHA_1.get(1), result.get(1));

    // insert metric values 2 and 3 using direct service call
    // this should result in 1, 2, and 3 all being stored
    appdynamicsService.saveMetricData(ACCOUNT_ID, appId, tier.getId(), Arrays.asList(METRIC_DATA_ALPHA_2));
    response = wingsPersistence.query(AppdynamicsMetricDataRecord.class, requestBuilder.build());
    result = response.getResponse();
    Assert.assertEquals("result not correct length", 3, result.size());

    // more specific query
    requestBuilder.addFilter("btName", Operator.EQ, btName)
        .addFilter("nodeName", Operator.EQ, "alpha")
        .addFilter("startTime", Operator.EQ, 1495432894010L);
    response = wingsPersistence.query(AppdynamicsMetricDataRecord.class, requestBuilder.build());
    result = response.getResponse();
    Assert.assertEquals("short result not correct length", 1, result.size());

    // generate metrics using the REST interface
    // insert beta nodes
    appdynamicsService.saveMetricData(
        ACCOUNT_ID, appId, tier.getId(), Arrays.asList(METRIC_DATA_BETA_1, METRIC_DATA_BETA_2));

    // requires sample StateExecutionInstance
    Query<Application> appQuery = wingsPersistence.createQuery(Application.class);
    appQuery.filter("accountId = ", ACCOUNT_ID);
    Application app = wingsPersistence.executeGetOneQuery(appQuery);
    AppDynamicsExecutionData aed = AppDynamicsExecutionData.Builder.anAppDynamicsExecutionData()
                                       .withAppDynamicsApplicationId(appId)
                                       .withAppdynamicsTierId(tier.getId())
                                       .withBtNames(Arrays.asList(btName))
                                       .withCanaryNewHostNames(Arrays.asList("beta"))
                                       .withStartTs(METRIC_VALUE_1.getStartTimeInMillis())
                                       .withEndTs(METRIC_VALUE_3.getStartTimeInMillis() + TimeUnit.MINUTES.toMillis(1))
                                       .withCorrelationId("correlation_id")
                                       .withStateExecutionInstanceId("state_execution_instance_id")
                                       .withAppDynamicsConfigID(settingId)
                                       .build();
    StateExecutionInstance sei = StateExecutionInstance.Builder.aStateExecutionInstance()
                                     .addStateExecutionData(StateType.APP_DYNAMICS.getName(), aed)
                                     .withAppId(app.getAppId())
                                     .withUuid("state_execution_instance_id")
                                     .build();
    wingsPersistence.save(sei);
    target = client.target(API_BASE + "/appdynamics/generate-metrics?stateExecutionId="
        + "state_execution_instance_id"
        + "&accountId=" + ACCOUNT_ID + "&appId=" + app.getAppId());

    RestResponse<MetricSummary> metricRestResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<MetricSummary>>() {});
    MetricSummary generatedMetrics = metricRestResponse.getResource();
    Assert.assertEquals(
        RiskLevel.LOW, generatedMetrics.getBtMetricsMap().get(btName).getMetricsMap().get(METRIC_NAME).getRisk());
    Assert.assertEquals(200,
        generatedMetrics.getBtMetricsMap().get(btName).getMetricsMap().get(METRIC_NAME).getOldData().getValue(), 0.05);

    // delete
    query = wingsPersistence.createQuery(AppdynamicsMetricDataRecord.class);
    query.filter("accountId = ", ACCOUNT_ID)
        .filter("appdAppId = ", appId)
        .filter("metricId", METRIC_ID)
        .filter("tierId", tier.getId());
    success = wingsPersistence.delete(query);
    assert (success);
    requestBuilder = aPageRequest()
                         .addFilter("accountId", Operator.EQ, ACCOUNT_ID)
                         .addFilter("appdAppId", Operator.EQ, appId)
                         .addFilter("metricId", Operator.EQ, METRIC_ID)
                         .addFilter("tierId", Operator.EQ, tier.getId())
                         .addOrder("startTime", OrderType.ASC);
    response = wingsPersistence.query(AppdynamicsMetricDataRecord.class, requestBuilder.build());
    result = response.getResponse();
    Assert.assertEquals("result not correct length", 0, result.size());

    success = wingsPersistence.delete(StateExecutionInstance.class, "state_execution_instance_id");
    assert (success);
  }
}

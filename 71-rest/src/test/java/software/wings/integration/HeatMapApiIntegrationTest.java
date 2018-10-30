package software.wings.integration;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import org.junit.Before;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLHostSummary;
import software.wings.service.impl.analysis.TimeSeriesMLMetricSummary;
import software.wings.service.impl.analysis.TimeSeriesMLTxnSummary;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesDataPoint;
import software.wings.verification.dashboard.HeatMapUnit;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * @author Vaibhav Tulsyan
 * 22/Oct/2018
 */
public class HeatMapApiIntegrationTest extends BaseIntegrationTest {
  @Inject WingsPersistence wingsPersistence;

  private NewRelicCVServiceConfiguration newRelicCVServiceConfiguration;
  private SettingAttribute settingAttribute;
  private String settingAttributeId;
  private Application savedApp;
  private Service savedService;
  private Environment savedEnv;

  private long endTime = System.currentTimeMillis();
  private long start12HoursAgo = endTime - TimeUnit.HOURS.toMillis(12);
  private long start1DayAgo = endTime - TimeUnit.DAYS.toMillis(1);
  private long start7DaysAgo = endTime - TimeUnit.DAYS.toMillis(7);
  private long start30DaysAgo = endTime - TimeUnit.DAYS.toMillis(30);

  @Before
  public void setUp() {
    loginAdminUser();

    savedApp = wingsPersistence.saveAndGet(Application.class,
        Application.Builder.anApplication().withName(generateUuid()).withAccountId(accountId).build());

    savedService = wingsPersistence.saveAndGet(
        Service.class, Service.builder().name(generateUuid()).appId(savedApp.getUuid()).build());

    savedEnv = wingsPersistence.saveAndGet(Environment.class,
        Environment.Builder.anEnvironment().withName(generateUuid()).withAppId(savedApp.getUuid()).build());

    settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                           .withAccountId(accountId)
                           .withName("someSettingAttributeName")
                           .withCategory(Category.CONNECTOR)
                           .withEnvId(savedEnv.getUuid())
                           .withAppId(savedApp.getUuid())
                           .build();
    settingAttributeId = wingsPersistence.saveAndGet(SettingAttribute.class, settingAttribute).getUuid();

    createNewRelicConfig();
    generate30DaysRandomData();
  }

  private void createNewRelicConfig() {
    newRelicCVServiceConfiguration = new NewRelicCVServiceConfiguration();
    newRelicCVServiceConfiguration.setName(generateUuid());
    newRelicCVServiceConfiguration.setAppId(savedApp.getUuid());
    newRelicCVServiceConfiguration.setEnvId(savedEnv.getUuid());
    newRelicCVServiceConfiguration.setEnvName(savedEnv.getName());
    newRelicCVServiceConfiguration.setServiceId(savedService.getUuid());
    newRelicCVServiceConfiguration.setServiceName(savedService.getName());
    newRelicCVServiceConfiguration.setStateType(StateType.NEW_RELIC);
    newRelicCVServiceConfiguration.setEnabled24x7(true);
    newRelicCVServiceConfiguration.setApplicationId(generateUuid());
    newRelicCVServiceConfiguration.setConnectorName(settingAttribute.getName());
    newRelicCVServiceConfiguration.setConnectorId(settingAttributeId);
    newRelicCVServiceConfiguration.setMetrics(Collections.singletonList("apdexScore"));
    newRelicCVServiceConfiguration.setAnalysisTolerance(AnalysisTolerance.MEDIUM);

    newRelicCVServiceConfiguration = (NewRelicCVServiceConfiguration) wingsPersistence.saveAndGet(
        CVConfiguration.class, newRelicCVServiceConfiguration);
  }

  private void generate30DaysRandomData() {
    // Add time series analysis records for each minute in 30 days
    int currentMinute = (int) TimeUnit.MILLISECONDS.toMinutes(start30DaysAgo);
    int minutesIn30Days = (int) TimeUnit.DAYS.toMinutes(30);
    logger.info("Creating {} units", minutesIn30Days);

    Random random = new Random();
    for (int i = 0; i < minutesIn30Days; i++) {
      TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
      timeSeriesMLAnalysisRecord.setAppId(savedApp.getUuid());
      timeSeriesMLAnalysisRecord.setAnalysisMinute(currentMinute + i);
      timeSeriesMLAnalysisRecord.setCvConfigId(newRelicCVServiceConfiguration.getUuid());
      timeSeriesMLAnalysisRecord.setStateType(StateType.APP_DYNAMICS);

      Map<String, TimeSeriesMLTxnSummary> txnMap = new HashMap<>();
      TimeSeriesMLTxnSummary txnSummary = new TimeSeriesMLTxnSummary();

      // TODO: Add test for 1 more txn name
      txnSummary.setTxn_name("/login");
      txnSummary.setGroup_name("default");
      txnSummary.setGroup_name(generateUuid());

      Map<String, TimeSeriesMLMetricSummary> summary = new HashMap<>();
      TimeSeriesMLMetricSummary metricSummary = new TimeSeriesMLMetricSummary();
      metricSummary.setMetric_name("apdexScore");

      TimeSeriesMLHostSummary timeSeriesMLHostSummary;
      int risk = -1; // NA by default

      if (i >= minutesIn30Days - (VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES) * 32) {
        risk = 2;
      } else if (i >= minutesIn30Days - 2 * (VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES) * 32) {
        risk = 0;
      } else {
        int randomNum = random.nextInt(2);
        if (randomNum % 2 == 0) {
          risk = random.nextInt(3);
        }
      }

      timeSeriesMLHostSummary = TimeSeriesMLHostSummary.builder().risk(risk).build();
      Map<String, TimeSeriesMLHostSummary> results = new HashMap<>();
      results.put(generateUuid(), timeSeriesMLHostSummary);
      metricSummary.setResults(results);

      summary.put("0", metricSummary);
      txnSummary.setMetrics(summary);
      txnMap.put("0", txnSummary);
      timeSeriesMLAnalysisRecord.setTransactions(txnMap);

      try {
        wingsPersistence.save(timeSeriesMLAnalysisRecord);
      } catch (DuplicateKeyException e) {
        // Eating exception and not logging because there would be too many logs (one per record)
        // Hitting this point is expected in all tests after the first test.
      }

      // Add new relic metric record for minute=currentMinute + i
      NewRelicMetricDataRecord newRelicMetricDataRecord = NewRelicMetricDataRecord.builder()
                                                              .appId(savedApp.getUuid())
                                                              .groupName("default")
                                                              .name("/login")
                                                              .serviceId(savedService.getUuid())
                                                              .stateType(StateType.APP_DYNAMICS)
                                                              .dataCollectionMinute(currentMinute + i)
                                                              .timeStamp(TimeUnit.MINUTES.toMillis(currentMinute + i))
                                                              .host(generateUuid())
                                                              .workflowExecutionId(generateUuid())
                                                              .stateExecutionId(generateUuid())
                                                              .cvConfigId(newRelicCVServiceConfiguration.getUuid())
                                                              .build();

      double val = 0.5 + random.nextDouble() * 10;
      Map<String, Double> valueMap = new HashMap<>();
      valueMap.put("apdexScore", val);

      val = 0.5 + random.nextDouble() * 10;
      valueMap.put("averageResponseTime", val);
      newRelicMetricDataRecord.setValues(valueMap);
      wingsPersistence.save(newRelicMetricDataRecord);
    }
  }

  //  @Test
  public void testHeatMapSummary() {
    String url = API_BASE + "/cvdash" + VerificationConstants.HEATMAP_SUMMARY + "?accountId=" + accountId
        + "&appId=" + savedApp.getUuid() + "&serviceId=" + savedService.getUuid() + "&startTime=" + start1DayAgo
        + "&endTime=" + endTime;
    logger.info("GET {}", url);
    WebTarget target = client.target(url);
    RestResponse<Map<String, List<HeatMap>>> response =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<Map<String, List<HeatMap>>>>() {});
    Map<String, List<HeatMap>> fetchedObject = response.getResource();
    assertTrue(fetchedObject.containsKey(savedEnv.getName())); // key should be env name

    List<HeatMap> heatmaps = fetchedObject.get(savedEnv.getName());
    HeatMap heatMap = heatmaps.get(0);
    List<HeatMapUnit> heatMapUnits = heatMap.getRiskLevelSummary();
    assertEquals(96, heatMapUnits.size());
    assertEquals(1, heatMapUnits.get(heatMapUnits.size() - 1).getHighRisk());
    assertEquals(0, heatMapUnits.get(heatMapUnits.size() - 1).getMediumRisk());
    assertEquals(0, heatMapUnits.get(heatMapUnits.size() - 1).getLowRisk());
    assertEquals(0, heatMapUnits.get(heatMapUnits.size() - 1).getNa());
  }

  //  @Test
  public void testTimeSeries() {
    String url = API_BASE + "/cvdash" + VerificationConstants.TIMESERIES + "?accountId=" + accountId
        + "&appId=" + savedApp.getUuid() + "&cvConfigId=" + newRelicCVServiceConfiguration.getUuid()
        + "&startTime=" + start12HoursAgo + "&endTime=" + endTime;

    WebTarget target = client.target(url);
    RestResponse<Map<String, Map<String, List<TimeSeriesDataPoint>>>> response =
        getRequestBuilderWithAuthHeader(target).get(
            new GenericType<RestResponse<Map<String, Map<String, List<TimeSeriesDataPoint>>>>>() {});
    Map<String, Map<String, List<TimeSeriesDataPoint>>> fetchedObject = response.getResource();
    assertTrue(fetchedObject.containsKey("/login"));

    Map<String, List<TimeSeriesDataPoint>> allMetrics = fetchedObject.get("/login");
    assertTrue(allMetrics.containsKey("apdexScore"));
    assertEquals(720, allMetrics.get("apdexScore").size());
  }
}

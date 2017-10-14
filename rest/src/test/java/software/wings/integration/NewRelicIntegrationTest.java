package software.wings.integration;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 9/7/17.
 */
public class NewRelicIntegrationTest extends BaseIntegrationTest {
  private Set<String> hosts = new HashSet<>();
  @Inject private NewRelicService newRelicService;
  @Inject private SettingsService settingsService;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(NewRelicMetricDataRecord.class));
    deleteAllDocuments(Arrays.asList(NewRelicMetricAnalysisRecord.class));
    deleteAllDocuments(Arrays.asList(SettingAttribute.class));
    hosts.clear();
    hosts.add("ip-172-31-2-144");
    hosts.add("ip-172-31-4-253");
    hosts.add("ip-172-31-12-51");

    SettingAttribute newRelicSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("NewRelic")
            .withAccountId(accountId)
            .withValue(NewRelicConfig.builder()
                           .accountId(accountId)
                           .newRelicUrl("https://api.newrelic.com")
                           .apiKey("5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11".toCharArray())
                           .build())
            .build();
    wingsPersistence.saveAndGet(SettingAttribute.class, newRelicSettingAttribute);
  }

  @Test
  public void getNewRelicApplications() throws Exception {
    // find new relic setting id

    PageRequest<SettingAttribute> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("name", EQ, "NewRelic").build();
    PageResponse<SettingAttribute> settings = settingsService.list(pageRequest);

    Assert.assertEquals(1, settings.size());

    WebTarget target = client.target(
        API_BASE + "/newrelic/applications?settingId=" + settings.get(0).getUuid() + "&accountId=" + accountId);
    RestResponse<List<NewRelicApplication>> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<List<NewRelicApplication>>>() {});

    Assert.assertEquals(0, restResponse.getResponseMessages().size());
    Assert.assertTrue(restResponse.getResource().size() > 0);

    for (NewRelicApplication app : restResponse.getResource()) {
      Assert.assertTrue(app.getId() > 0);
      Assert.assertFalse(StringUtils.isBlank(app.getName()));
    }
  }

  @Test
  public void testMetricSave() throws Exception {
    final int numOfMinutes = 4;
    final int numOfBatches = 5;
    final int numOfMetricsPerBatch = 100;
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String serviceId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = UUID.randomUUID().toString();

    Random r = new Random();

    for (int batchNum = 0; batchNum < numOfBatches; batchNum++) {
      List<NewRelicMetricDataRecord> metricDataRecords = new ArrayList<>();

      for (int metricNum = 0; metricNum < numOfMetricsPerBatch; metricNum++) {
        String metricName = "metric-" + batchNum * numOfMetricsPerBatch + metricNum;
        for (String host : hosts) {
          for (int collectionMin = 0; collectionMin < numOfMinutes; collectionMin++) {
            NewRelicMetricDataRecord record = new NewRelicMetricDataRecord();
            record.setName(metricName);
            record.setHost(host);
            record.setWorkflowId(workflowId);
            record.setWorkflowExecutionId(workflowExecutionId);
            record.setServiceId(serviceId);
            record.setStateExecutionId(stateExecutionId);
            record.setTimeStamp(collectionMin);
            record.setDataCollectionMinute(collectionMin);

            record.setThroughput(r.nextDouble());
            record.setAverageResponseTime(r.nextDouble());
            record.setError(r.nextDouble());
            record.setApdexScore(r.nextDouble());

            metricDataRecords.add(record);

            // add more records for duplicate records for the same time
            if (collectionMin > 0) {
              record = new NewRelicMetricDataRecord();
              record.setName(metricName);
              record.setHost(host);
              record.setWorkflowId(workflowId);
              record.setWorkflowExecutionId(workflowExecutionId);
              record.setServiceId(serviceId);
              record.setStateExecutionId(stateExecutionId);
              // duplicate for previous minute
              record.setTimeStamp(collectionMin - 1);
              record.setDataCollectionMinute(collectionMin);

              record.setThroughput(r.nextDouble());
              record.setAverageResponseTime(r.nextDouble());
              record.setError(r.nextDouble());
              record.setApdexScore(r.nextDouble());

              metricDataRecords.add(record);
            }
          }
        }
      }

      WebTarget target =
          client.target(API_BASE + "/newrelic/save-metrics?accountId=" + accountId + "&applicationId=" + applicationId);
      RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
          Entity.entity(metricDataRecords, APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
      Assert.assertTrue(restResponse.getResource());

      Query<NewRelicMetricDataRecord> query = wingsPersistence.createQuery(NewRelicMetricDataRecord.class);
      Assert.assertEquals((batchNum + 1) * numOfMetricsPerBatch * hosts.size() * numOfMinutes, query.asList().size());
    }
  }

  @Test
  public void testAnalysisSorted() throws Exception {
    final String workflowId = UUID.randomUUID().toString();
    final String workflowExecutionId = UUID.randomUUID().toString();
    final String stateExecutionId = UUID.randomUUID().toString();
    final String applicationId = UUID.randomUUID().toString();

    final NewRelicMetricAnalysisRecord record = NewRelicMetricAnalysisRecord.builder()
                                                    .workflowId(workflowId)
                                                    .workflowExecutionId(workflowExecutionId)
                                                    .stateExecutionId(stateExecutionId)
                                                    .applicationId(applicationId)
                                                    .stateType(StateType.NEW_RELIC)
                                                    .metricAnalyses(new ArrayList<>())
                                                    .build();

    final NewRelicMetricAnalysis analysis1 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.HIGH).metricName("metric1").build();
    record.addNewRelicMetricAnalysis(analysis1);

    final NewRelicMetricAnalysis analysis2 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.MEDIUM).metricName("metric1").build();
    record.addNewRelicMetricAnalysis(analysis2);

    final NewRelicMetricAnalysis analysis3 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.LOW).metricName("metric1").build();
    record.addNewRelicMetricAnalysis(analysis3);

    final NewRelicMetricAnalysis analysis4 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.LOW).metricName("metric0").build();
    record.addNewRelicMetricAnalysis(analysis4);

    final NewRelicMetricAnalysis analysis5 =
        NewRelicMetricAnalysis.builder().riskLevel(RiskLevel.LOW).metricName("abc").build();
    record.addNewRelicMetricAnalysis(analysis5);

    wingsPersistence.save(record);

    WebTarget target = client.target(API_BASE + "/newrelic/generate-metrics?accountId=" + accountId
        + "&stateExecutionId=" + stateExecutionId + "&workflowExecutionId=" + workflowExecutionId);
    RestResponse<NewRelicMetricAnalysisRecord> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<NewRelicMetricAnalysisRecord>>() {});

    NewRelicMetricAnalysisRecord savedRecord = restResponse.getResource();
    Assert.assertNotNull(savedRecord);

    final List<NewRelicMetricAnalysis> analyses = savedRecord.getMetricAnalyses();
    Assert.assertEquals(record.getMetricAnalyses().size(), analyses.size());

    Assert.assertEquals(analysis1, analyses.get(0));
    Assert.assertEquals(analysis2, analyses.get(1));
    Assert.assertEquals(analysis5, analyses.get(2));
    Assert.assertEquals(analysis4, analyses.get(3));
    Assert.assertEquals(analysis3, analyses.get(4));
  }
}

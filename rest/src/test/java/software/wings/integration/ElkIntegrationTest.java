package software.wings.integration;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import software.wings.beans.RestResponse;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.analysis.AnalysisServiceImpl;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.AnalysisService;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.states.AbstractLogAnalysisState;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 8/17/17.
 */
public class ElkIntegrationTest extends BaseIntegrationTest {
  private Set<String> hosts = new HashSet<>();
  @Inject private AnalysisService analysisService;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(LogDataRecord.class));
    deleteAllDocuments(Arrays.asList(WorkflowExecution.class));
    deleteAllDocuments(Arrays.asList(Workflow.class));
    hosts.clear();
    hosts.add("ip-172-31-2-144");
    hosts.add("ip-172-31-4-253");
    hosts.add("ip-172-31-12-51");
    hosts.add("ip-172-31-12-78");
    hosts.add("ip-172-31-15-177");
  }

  @Test
  public void testFirstLevelClustering() throws Exception {
    for (String host : hosts) {
      File file = new File(getClass().getClassLoader().getResource("./elk/" + host + ".json").getFile());

      List<LogDataRecord> logDataRecords = readLogDataRecordsFromFile(file);
      Map<Integer, List<LogElement>> recordsByMinute = splitRecordsByMinute(logDataRecords);

      final String stateExecutionId = "some-state-execution";
      final String workflowId = "some-workflow";
      final String workflowExecutionId = "some-workflow-execution";
      final String applicationId = "some-application";
      final String serviceId = "some-service";
      final String query = ".*exception.*";

      WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
          + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL + "?accountId=" + accountId + "&clusterLevel="
          + ClusterLevel.L0.name() + "&stateExecutionId=" + stateExecutionId + "&workflowId=" + workflowId
          + "&workflowExecutionId=" + workflowExecutionId + "&appId=" + applicationId + "&serviceId=" + serviceId);

      for (Entry<Integer, List<LogElement>> entry : recordsByMinute.entrySet()) {
        RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
            Entity.entity(entry.getValue(), APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
        Assert.assertTrue(restResponse.getResource());
      }

      Thread.sleep(TimeUnit.SECONDS.toMillis(40));
      for (int collectionMinute = 0; collectionMinute < recordsByMinute.size(); collectionMinute++) {
        WebTarget getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
            + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + accountId
            + "&clusterLevel=" + ClusterLevel.L0.name() + "&compareCurrent=true");
        final LogRequest logRequest = new LogRequest(query, applicationId, stateExecutionId, workflowId, serviceId,
            Collections.singleton(host), collectionMinute);
        RestResponse<List<LogDataRecord>> restResponse = getRequestBuilderWithAuthHeader(getTarget).post(
            Entity.entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<LogDataRecord>>>() {});
        // no L0 level data should be present
        Assert.assertEquals(0, restResponse.getResource().size());

        getTarget = client.target(API_BASE + "/" + LogAnalysisResource.ELK_RESOURCE_BASE_URL
            + LogAnalysisResource.ANALYSIS_STATE_GET_LOG_URL + "?accountId=" + accountId
            + "&clusterLevel=" + ClusterLevel.L1.name() + "&compareCurrent=true");
        restResponse = getRequestBuilderWithAuthHeader(getTarget).post(
            Entity.entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<LogDataRecord>>>() {});
        Assert.assertEquals(
            "failed for " + host + " for minute " + collectionMinute, 15, restResponse.getResource().size());
      }
    }
  }

  private Map<Integer, List<LogElement>> splitRecordsByMinute(List<LogDataRecord> logDataRecords) {
    final Map<Integer, List<LogElement>> rv = new HashMap<>();
    for (LogDataRecord logDataRecord : logDataRecords) {
      if (!rv.containsKey(logDataRecord.getLogCollectionMinute())) {
        rv.put(logDataRecord.getLogCollectionMinute(), new ArrayList<>());
      }

      rv.get(logDataRecord.getLogCollectionMinute())
          .add(new LogElement(logDataRecord.getQuery(), logDataRecord.getClusterLabel(), logDataRecord.getHost(),
              logDataRecord.getTimeStamp(), logDataRecord.getCount(), logDataRecord.getLogMessage(),
              logDataRecord.getLogCollectionMinute()));
    }
    return rv;
  }

  private List<LogDataRecord> readLogDataRecordsFromFile(File file) throws FileNotFoundException {
    final Gson gson = new Gson();
    BufferedReader br = new BufferedReader(new FileReader(file));
    Type type = new TypeToken<List<LogDataRecord>>() {}.getType();
    return gson.fromJson(br, type);
  }

  @Test
  public void testPurge() throws Exception {
    final String applicationId = "some-application";
    final String serviceId = "some-service";
    int numOfWorkFlows = 3;
    int numOfExecutionPerWorkFlow = 5;
    int numOfMinutes = 10;
    int numOfLogs = 12;

    Map<String, String> workFlowToExecution = new HashMap<>();
    Map<String, String> workFlowToStateExecution = new HashMap<>();
    int totalRecordsInserted = 0;
    for (int workflowNum = 0; workflowNum < numOfWorkFlows; workflowNum++) {
      Workflow workflow = aWorkflow().withAppId(applicationId).withName("workflow-" + workflowNum).build();
      String workFlowId = wingsPersistence.save(workflow);
      for (int executionNum = 0; executionNum < numOfExecutionPerWorkFlow; executionNum++) {
        WorkflowExecution workflowExecution = aWorkflowExecution()
                                                  .withWorkflowId(workFlowId)
                                                  .withAppId(applicationId)
                                                  .withName(workFlowId + "-execution-" + executionNum)
                                                  .withStatus(ExecutionStatus.SUCCESS)
                                                  .build();
        String workFlowExecutionId = wingsPersistence.save(workflowExecution);
        workFlowToExecution.put(workFlowId, workFlowExecutionId);
        final String stateExecutionId = workFlowExecutionId + "-state-execution-" + executionNum;
        workFlowToStateExecution.put(workFlowId, stateExecutionId);
        for (StateType stateType : AnalysisServiceImpl.logAnalysisStates) {
          for (String host : hosts) {
            Map<Integer, List<LogElement>> recordsByMinute = generateLogElements(host, numOfMinutes, numOfLogs);
            WebTarget target = client.target(API_BASE + "/" + AbstractLogAnalysisState.getStateBaseUrl(stateType)
                + LogAnalysisResource.ANALYSIS_STATE_SAVE_LOG_URL + "?accountId=" + accountId
                + "&clusterLevel=" + ClusterLevel.L2.name() + "&stateExecutionId=" + stateExecutionId
                + "&workflowId=" + workFlowId + "&workflowExecutionId=" + workFlowExecutionId
                + "&appId=" + applicationId + "&serviceId=" + serviceId);

            for (Entry<Integer, List<LogElement>> entry : recordsByMinute.entrySet()) {
              totalRecordsInserted += entry.getValue().size();
              RestResponse<Boolean> restResponse = getDelegateRequestBuilderWithAuthHeader(target).post(
                  Entity.entity(entry.getValue(), APPLICATION_JSON), new GenericType<RestResponse<Boolean>>() {});
              Assert.assertTrue(restResponse.getResource());
            }
          }
        }
        System.out.println("done for workFlow: " + workFlowId + " execution: " + workFlowExecutionId);
      }
    }

    Query<LogDataRecord> logDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class);
    Assert.assertEquals(totalRecordsInserted, logDataRecordQuery.asList().size());
    analysisService.purgeLogs();
    logDataRecordQuery = wingsPersistence.createQuery(LogDataRecord.class);
    List<LogDataRecord> logDataRecords = logDataRecordQuery.asList();
    Assert.assertEquals(
        numOfMinutes * numOfLogs * hosts.size() * AnalysisServiceImpl.logAnalysisStates.length * numOfWorkFlows,
        logDataRecords.size());

    for (LogDataRecord logDataRecord : logDataRecords) {
      Assert.assertEquals(
          workFlowToExecution.get(logDataRecord.getWorkflowId()), logDataRecord.getWorkflowExecutionId());
      Assert.assertEquals(
          workFlowToStateExecution.get(logDataRecord.getWorkflowId()), logDataRecord.getStateExecutionId());
    }
  }

  private Map<Integer, List<LogElement>> generateLogElements(String host, int numOfMinutes, int numOfLogs) {
    long timeStamp = System.currentTimeMillis();
    Map<Integer, List<LogElement>> rv = new HashMap<>();
    for (int i = 0; i < numOfMinutes; i++) {
      rv.put(i, new ArrayList<>());

      long timeStampMin = timeStamp + TimeUnit.MINUTES.toMillis(i);
      for (int j = 0; j < numOfLogs; j++) {
        rv.get(i).add(new LogElement(
            "*exception", UUID.randomUUID().toString(), host, timeStampMin, 32, UUID.randomUUID().toString(), i));
      }
    }
    return rv;
  }
}

package software.wings.integration;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;

import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;

import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import software.wings.beans.RestResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.service.impl.analysis.LogDataRecord;
import software.wings.service.impl.analysis.LogRequest;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.service.intfc.analysis.LogAnalysisResource;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

/**
 * Created by rsingh on 7/13/17.
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SplunkIntegrationTest extends BaseIntegrationTest {
  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(asList(LogDataRecord.class));
    deleteAllDocuments(asList(WorkflowExecution.class));
  }

  @Test
  public void testGetCurrentExecutionLogs() throws Exception {
    final Random r = new Random();
    final int numOfExecutions = 4;
    final int numOfHosts = 3;
    final int numOfMinutes = 3;
    final int numOfRecords = 5;

    final String workflowId = "some-workflow";
    final String query = "some-query";
    final String applicationId = "some-application";
    final String serviceId = "some-service";
    final TreeBasedTable<Integer, Integer, List<LogDataRecord>> addedMessages = TreeBasedTable.create();
    final Set<String> hosts = new HashSet<>();

    WorkflowExecution workflowExecution = aWorkflowExecution()
                                              .withStatus(ExecutionStatus.SUCCESS)
                                              .withWorkflowId(workflowId)
                                              .withAppId(applicationId)
                                              .build();
    wingsPersistence.save(workflowExecution);
    for (int executionNumber = 1; executionNumber <= numOfExecutions; executionNumber++) {
      final String stateExecutionId = "se" + executionNumber;
      for (int hostNumber = 0; hostNumber < numOfHosts; hostNumber++) {
        final String host = "host" + hostNumber;
        hosts.add(host);
        for (int logCollectionMinute = 0; logCollectionMinute < numOfMinutes; logCollectionMinute++) {
          final long timeStamp = System.currentTimeMillis();
          for (int recordNumber = 0; recordNumber < numOfRecords; recordNumber++) {
            final int count = r.nextInt();
            final String logMessage = "lmsg" + recordNumber;
            final String logMD5Hash = "lmsgHash" + recordNumber;
            final String clusterLabel = "cluster" + recordNumber;

            final LogDataRecord logDataRecord = new LogDataRecord();
            logDataRecord.setStateType(StateType.SPLUNKV2);
            logDataRecord.setWorkflowId(workflowId);
            logDataRecord.setWorkflowExecutionId(workflowExecution.getUuid());
            logDataRecord.setStateExecutionId(stateExecutionId);
            logDataRecord.setQuery(query);
            logDataRecord.setApplicationId(applicationId);
            logDataRecord.setClusterLabel(clusterLabel);
            logDataRecord.setHost(host);
            logDataRecord.setTimeStamp(timeStamp);
            logDataRecord.setCount(count);
            logDataRecord.setLogMessage(logMessage);
            logDataRecord.setLogMD5Hash(logMD5Hash);
            logDataRecord.setClusterLevel(ClusterLevel.L0);
            logDataRecord.setLogCollectionMinute(logCollectionMinute);
            logDataRecord.setCreatedAt(timeStamp);
            logDataRecord.setServiceId(serviceId);

            wingsPersistence.save(logDataRecord);

            if (addedMessages.get(executionNumber, logCollectionMinute) == null) {
              addedMessages.put(executionNumber, logCollectionMinute, new ArrayList<>());
            }

            addedMessages.get(executionNumber, logCollectionMinute).add(logDataRecord);
          }
          Thread.sleep(10);
        }
      }
    }

    for (int collectionMinute = 0; collectionMinute < numOfMinutes; collectionMinute++) {
      WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL
          + "/get-logs?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L0.name()
          + "&compareCurrent=true&workflowExecutionId=" + workflowExecution.getUuid());
      final LogRequest logRequest =
          new LogRequest(query, applicationId, "se2", workflowId, serviceId, hosts, collectionMinute);
      RestResponse<List<LogDataRecord>> restResponse = getRequestBuilderWithAuthHeader(target).post(
          entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<LogDataRecord>>>() {});
      assertEquals(
          "failed for minute " + collectionMinute, addedMessages.get(2, collectionMinute), restResponse.getResource());
    }
  }

  @Test
  public void testGetLastExecutionLogs() throws Exception {
    final Random r = new Random();
    final int numOfExecutions = 1;
    final int numOfHosts = 1 + r.nextInt(5);
    final int numOfMinutes = 1 + r.nextInt(10);
    final int numOfRecords = 1 + r.nextInt(10);

    final String workflowId = "some-workflow";
    final String query = "some-query";
    final String applicationId = "some-application";
    final String serviceId = "some-service";
    final TreeBasedTable<Integer, Integer, List<LogDataRecord>> addedMessages = TreeBasedTable.create();

    WorkflowExecution workflowExecution = aWorkflowExecution()
                                              .withStatus(ExecutionStatus.SUCCESS)
                                              .withWorkflowId(workflowId)
                                              .withAppId(applicationId)
                                              .withStateMachineId(UUID.randomUUID().toString())
                                              .build();
    wingsPersistence.save(workflowExecution);
    StateMachine stateMachine = new StateMachine();
    stateMachine.setInitialStateName("some-state");
    stateMachine.setStates(Lists.newArrayList(new ApprovalState(stateMachine.getInitialStateName())));
    stateMachine.setUuid(workflowExecution.getStateMachineId());
    stateMachine.setAppId(applicationId);
    wingsPersistence.save(stateMachine);
    Set<String> hosts = new HashSet<>();

    for (int executionNumber = 1; executionNumber <= numOfExecutions; executionNumber++) {
      final String stateExecutionId = "se" + executionNumber;
      for (int hostNumber = 0; hostNumber < numOfHosts; hostNumber++) {
        final String host = "host" + hostNumber;
        hosts.add(host);
        for (int logCollectionMinute = 0; logCollectionMinute < numOfMinutes; logCollectionMinute++) {
          final long timeStamp = System.currentTimeMillis();
          for (int recordNumber = 0; recordNumber < numOfRecords; recordNumber++) {
            final int count = r.nextInt();
            final String logMessage = UUID.randomUUID().toString();
            final String logMD5Hash = UUID.randomUUID().toString();
            final String clusterLabel = UUID.randomUUID().toString();

            final LogDataRecord logDataRecord = new LogDataRecord();
            logDataRecord.setStateType(StateType.SPLUNKV2);
            logDataRecord.setWorkflowId(workflowId);
            logDataRecord.setStateExecutionId(stateExecutionId);
            logDataRecord.setQuery(query);
            logDataRecord.setApplicationId(applicationId);
            logDataRecord.setClusterLabel(clusterLabel);
            logDataRecord.setHost(host);
            logDataRecord.setTimeStamp(timeStamp);
            logDataRecord.setCount(count);
            logDataRecord.setLogMessage(logMessage);
            logDataRecord.setLogMD5Hash(logMD5Hash);
            logDataRecord.setClusterLevel(ClusterLevel.L0);
            logDataRecord.setLogCollectionMinute(logCollectionMinute);
            logDataRecord.setCreatedAt(timeStamp);
            logDataRecord.setServiceId(serviceId);
            logDataRecord.setWorkflowExecutionId(workflowExecution.getUuid());
            wingsPersistence.save(logDataRecord);

            if (addedMessages.get(executionNumber, logCollectionMinute) == null) {
              addedMessages.put(executionNumber, logCollectionMinute, new ArrayList<>());
            }

            addedMessages.get(executionNumber, logCollectionMinute).add(logDataRecord);
          }
          Thread.sleep(10);
        }
      }
    }

    for (int collectionMinute = 0; collectionMinute < numOfMinutes; collectionMinute++) {
      WebTarget target = client.target(API_BASE + "/" + LogAnalysisResource.SPLUNK_RESOURCE_BASE_URL
          + "/get-logs?accountId=" + accountId + "&clusterLevel=" + ClusterLevel.L0.name()
          + "&compareCurrent=false&workflowExecutionId=" + workflowExecution.getUuid());
      final LogRequest logRequest = new LogRequest(
          query, applicationId, UUID.randomUUID().toString(), workflowId, serviceId, hosts, collectionMinute);
      RestResponse<List<LogDataRecord>> restResponse = getRequestBuilderWithAuthHeader(target).post(
          entity(logRequest, APPLICATION_JSON), new GenericType<RestResponse<List<LogDataRecord>>>() {});
      assertEquals("failed for minute " + collectionMinute, addedMessages.get(numOfExecutions, collectionMinute),
          restResponse.getResource());
    }
  }
}

package io.harness.resource;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.VerificationBaseTest;
import io.harness.resources.TimeSeriesResource;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.RestResponse;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMLTransactionThresholds;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Vaibhav Tulsyan
 * 24/Sep/2018
 */
public class TimeSeriesResourceTest extends VerificationBaseTest {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private String delegateTaskId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String cvConfigIdId;
  private String groupName;
  private String baseLineExecutionId;
  private String cvConfigId;
  private StateType stateType = StateType.APP_DYNAMICS;
  private TSRequest tsRequest;
  private Set<String> nodes;
  private List<NewRelicMetricDataRecord> newRelicMetricDataRecords;
  private TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord;
  private List<TimeSeriesMLScores> timeSeriesMLScores;
  private Map<String, Map<String, TimeSeriesMetricDefinition>> metricTemplate;
  private String transactionName;
  private String metricName;
  private TimeSeriesMLTransactionThresholds timeSeriesMLTransactionThresholds;

  @Mock private TimeSeriesAnalysisService timeSeriesAnalysisService;

  private TimeSeriesResource timeSeriesResource;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    accountId = generateUuid();
    applicationId = generateUuid();
    stateExecutionId = generateUuid();
    delegateTaskId = generateUuid();
    workflowId = generateUuid();
    workflowExecutionId = generateUuid();
    baseLineExecutionId = generateUuid();
    serviceId = generateUuid();
    cvConfigIdId = generateUuid();
    cvConfigId = generateUuid();
    groupName = "groupName-";
    nodes = new HashSet<String>();
    nodes.add("someNode");
    newRelicMetricDataRecords = Collections.singletonList(new NewRelicMetricDataRecord());
    transactionName = UUID.randomUUID().toString();
    metricName = UUID.randomUUID().toString();

    timeSeriesResource = new TimeSeriesResource(timeSeriesAnalysisService);

    tsRequest = new TSRequest(stateExecutionId, workflowExecutionId, nodes, 0, 0);
    timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    timeSeriesMLScores = Collections.singletonList(new TimeSeriesMLScores());

    metricTemplate = Collections.singletonMap("key1", new HashMap<String, TimeSeriesMetricDefinition>());

    timeSeriesMLTransactionThresholds = TimeSeriesMLTransactionThresholds.builder().build();
  }

  @Test
  public void testSaveMetricData() throws IOException {
    when(timeSeriesAnalysisService.saveMetricData(
             accountId, applicationId, stateExecutionId, delegateTaskId, new ArrayList<>()))
        .thenReturn(true);
    RestResponse<Boolean> resp = timeSeriesResource.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, new ArrayList<>());
    assertTrue(resp.getResource());
  }

  @Test
  public void testGetMetricData() throws IOException {
    boolean compareCurrent = true;
    when(timeSeriesAnalysisService.getRecords(applicationId, stateExecutionId, groupName, nodes, 0, 0))
        .thenReturn(newRelicMetricDataRecords);
    RestResponse<List<NewRelicMetricDataRecord>> resp = timeSeriesResource.getMetricData(
        accountId, applicationId, workflowExecutionId, groupName, compareCurrent, tsRequest);
    assertEquals(Collections.singletonList(new NewRelicMetricDataRecord()), resp.getResource());

    resp = timeSeriesResource.getMetricData(accountId, applicationId, null, groupName, false, tsRequest);
    assertEquals(new ArrayList<>(), resp.getResource());

    when(timeSeriesAnalysisService.getPreviousSuccessfulRecords(applicationId, workflowExecutionId, groupName, 0, 0))
        .thenReturn(newRelicMetricDataRecords);
    resp = timeSeriesResource.getMetricData(accountId, applicationId, workflowExecutionId, groupName, false, tsRequest);
    assertEquals(newRelicMetricDataRecords, resp.getResource());

    verify(timeSeriesAnalysisService, times(1)).getRecords(applicationId, stateExecutionId, groupName, nodes, 0, 0);

    verify(timeSeriesAnalysisService, times(1))
        .getPreviousSuccessfulRecords(applicationId, workflowExecutionId, groupName, 0, 0);
  }

  @Test
  public void testSaveMLAnalysisRecords() throws IOException {
    when(timeSeriesAnalysisService.saveAnalysisRecordsML(accountId, stateType, applicationId, stateExecutionId,
             workflowExecutionId, groupName, 0, delegateTaskId, baseLineExecutionId, cvConfigId,
             timeSeriesMLAnalysisRecord))
        .thenReturn(true);
    RestResponse<Boolean> resp = timeSeriesResource.saveMLAnalysisRecords(accountId, applicationId, stateType,
        stateExecutionId, workflowExecutionId, groupName, 0, delegateTaskId, baseLineExecutionId, cvConfigId,
        timeSeriesMLAnalysisRecord);
    assertTrue(resp.getResource());
  }

  @Test
  public void testGetScores() throws IOException {
    when(timeSeriesAnalysisService.getTimeSeriesMLScores(applicationId, workflowId, 0, 1))
        .thenReturn(timeSeriesMLScores);
    RestResponse<List<TimeSeriesMLScores>> resp =
        timeSeriesResource.getScores(accountId, applicationId, workflowId, 0, 1);
    assertEquals(timeSeriesMLScores, resp.getResource());
  }

  @Test
  public void testGetMetricTemplate() {
    when(timeSeriesAnalysisService.getMetricTemplate(
             applicationId, stateType, stateExecutionId, serviceId, cvConfigId, groupName))
        .thenReturn(metricTemplate);
    RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> resp = timeSeriesResource.getMetricTemplate(
        accountId, applicationId, stateType, stateExecutionId, serviceId, cvConfigId, groupName);
    assertEquals(metricTemplate, resp.getResource());
  }
}

package io.harness.resource;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.resources.DelegateDataCollectionResource;
import io.harness.resources.TimeSeriesResource;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.FeatureName;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
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
  private String groupName;
  private String baseLineExecutionId;
  private String cvConfigId;
  private StateType stateType = StateType.APP_DYNAMICS;
  private TSRequest tsRequest;
  private Set<String> nodes;
  private Set<NewRelicMetricDataRecord> newRelicMetricDataRecords;
  private TimeSeriesMLAnalysisRecord timeSeriesMLAnalysisRecord;
  private List<TimeSeriesMLScores> timeSeriesMLScores;
  private Map<String, Map<String, TimeSeriesMetricDefinition>> metricTemplate;

  @Mock private TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Mock private VerificationManagerClient managerClient;
  @Mock private VerificationManagerClientHelper managerClientHelper;

  private TimeSeriesResource timeSeriesResource;
  @Inject private DelegateDataCollectionResource delegateDataCollectionResource;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    accountId = generateUuid();
    applicationId = generateUuid();
    stateExecutionId = generateUuid();
    delegateTaskId = generateUuid();
    workflowId = generateUuid();
    workflowExecutionId = generateUuid();
    baseLineExecutionId = generateUuid();
    serviceId = generateUuid();
    cvConfigId = generateUuid();
    groupName = "groupName-";
    nodes = new HashSet<>();
    nodes.add("someNode");
    newRelicMetricDataRecords = Sets.newHashSet(new NewRelicMetricDataRecord());

    timeSeriesResource = new TimeSeriesResource(timeSeriesAnalysisService, managerClientHelper, managerClient);

    tsRequest = new TSRequest(stateExecutionId, workflowExecutionId, nodes, 0, 0);
    timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    timeSeriesMLScores = Collections.singletonList(new TimeSeriesMLScores());

    metricTemplate = Collections.singletonMap("key1", new HashMap<>());
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveMetricData() throws IllegalAccessException {
    when(timeSeriesAnalysisService.saveMetricData(
             accountId, applicationId, stateExecutionId, delegateTaskId, new ArrayList<>()))
        .thenReturn(true);
    FieldUtils.writeField(delegateDataCollectionResource, "timeSeriesAnalysisService", timeSeriesAnalysisService, true);
    RestResponse<Boolean> resp = delegateDataCollectionResource.saveMetricData(
        accountId, applicationId, stateExecutionId, delegateTaskId, new ArrayList<>());
    assertThat(resp.getResource()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricData() throws IOException {
    boolean compareCurrent = true;
    when(timeSeriesAnalysisService.getRecords(applicationId, stateExecutionId, groupName, nodes, 0, 0))
        .thenReturn(newRelicMetricDataRecords);
    RestResponse<Set<NewRelicMetricDataRecord>> resp = timeSeriesResource.getMetricData(
        accountId, applicationId, workflowExecutionId, groupName, compareCurrent, tsRequest);
    assertEquals(Sets.newHashSet(new NewRelicMetricDataRecord()), resp.getResource());

    resp = timeSeriesResource.getMetricData(accountId, applicationId, null, groupName, false, tsRequest);
    assertEquals(new HashSet<>(), resp.getResource());

    when(timeSeriesAnalysisService.getPreviousSuccessfulRecords(applicationId, workflowExecutionId, groupName, 0, 0))
        .thenReturn(newRelicMetricDataRecords);
    resp = timeSeriesResource.getMetricData(accountId, applicationId, workflowExecutionId, groupName, false, tsRequest);
    assertEquals(newRelicMetricDataRecords, resp.getResource());

    verify(timeSeriesAnalysisService, times(1)).getRecords(applicationId, stateExecutionId, groupName, nodes, 0, 0);

    verify(timeSeriesAnalysisService, times(1))
        .getPreviousSuccessfulRecords(applicationId, workflowExecutionId, groupName, 0, 0);
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveMLAnalysisRecords() throws IOException {
    when(timeSeriesAnalysisService.saveAnalysisRecordsML(accountId, stateType, applicationId, stateExecutionId,
             workflowExecutionId, groupName, 0, delegateTaskId, baseLineExecutionId, cvConfigId,
             timeSeriesMLAnalysisRecord, null))
        .thenReturn(true);
    RestResponse<Boolean> resp = timeSeriesResource.saveMLAnalysisRecords(accountId, applicationId, stateType,
        stateExecutionId, workflowExecutionId, groupName, 0, delegateTaskId, baseLineExecutionId, cvConfigId, null,
        timeSeriesMLAnalysisRecord);
    assertThat(resp.getResource()).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetScores() throws IOException {
    when(timeSeriesAnalysisService.getTimeSeriesMLScores(applicationId, workflowId, 0, 1))
        .thenReturn(timeSeriesMLScores);
    RestResponse<List<TimeSeriesMLScores>> resp =
        timeSeriesResource.getScores(accountId, applicationId, workflowId, 0, 1);
    assertEquals(timeSeriesMLScores, resp.getResource());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetMetricTemplate() {
    when(timeSeriesAnalysisService.getMetricTemplate(
             applicationId, stateType, stateExecutionId, serviceId, cvConfigId, groupName))
        .thenReturn(metricTemplate);
    when(managerClientHelper.callManagerWithRetry(
             managerClient.isFeatureEnabled(FeatureName.SUPERVISED_TS_THRESHOLD, accountId)))
        .thenReturn(new RestResponse<>(false));
    RestResponse<Map<String, Map<String, TimeSeriesMetricDefinition>>> resp = timeSeriesResource.getMetricTemplate(
        accountId, applicationId, stateType, stateExecutionId, serviceId, cvConfigId, groupName);
    assertEquals(metricTemplate, resp.getResource());
  }
}

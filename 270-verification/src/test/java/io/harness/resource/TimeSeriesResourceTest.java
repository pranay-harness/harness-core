/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resource;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.SOWMYA;

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.VerificationBase;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.resources.DelegateDataCollectionResource;
import io.harness.resources.TimeSeriesResource;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;

import software.wings.dl.WingsPersistence;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.TSRequest;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Vaibhav Tulsyan
 * 24/Sep/2018
 */
public class TimeSeriesResourceTest extends VerificationBase {
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
  @Inject private LearningEngineService learningEngineService;

  @Inject private WingsPersistence wingsPersistence;

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

    timeSeriesResource =
        new TimeSeriesResource(timeSeriesAnalysisService, managerClientHelper, managerClient, learningEngineService);

    tsRequest = new TSRequest(stateExecutionId, workflowExecutionId, nodes, 0, 0);
    timeSeriesMLAnalysisRecord = TimeSeriesMLAnalysisRecord.builder().build();
    timeSeriesMLScores = Collections.singletonList(new TimeSeriesMLScores());

    metricTemplate = Collections.singletonMap("key1", new HashMap<>());
  }

  @Test
  @Owner(developers = PRAVEEN)
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
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetMetricData() throws IOException {
    boolean compareCurrent = true;
    when(timeSeriesAnalysisService.getRecords(applicationId, stateExecutionId, groupName, nodes, 0, 0, accountId))
        .thenReturn(newRelicMetricDataRecords);
    RestResponse<Set<NewRelicMetricDataRecord>> resp = timeSeriesResource.getMetricData(
        accountId, applicationId, workflowExecutionId, groupName, compareCurrent, tsRequest);
    assertThat(resp.getResource()).isEqualTo(Sets.newHashSet(new NewRelicMetricDataRecord()));

    resp = timeSeriesResource.getMetricData(accountId, applicationId, null, groupName, false, tsRequest);
    assertThat(resp.getResource()).isEqualTo(new HashSet<>());

    when(timeSeriesAnalysisService.getPreviousSuccessfulRecords(
             applicationId, workflowExecutionId, groupName, 0, 0, accountId))
        .thenReturn(newRelicMetricDataRecords);
    resp = timeSeriesResource.getMetricData(accountId, applicationId, workflowExecutionId, groupName, false, tsRequest);
    assertThat(resp.getResource()).isEqualTo(newRelicMetricDataRecords);

    verify(timeSeriesAnalysisService, times(1))
        .getRecords(applicationId, stateExecutionId, groupName, nodes, 0, 0, accountId);

    verify(timeSeriesAnalysisService, times(1))
        .getPreviousSuccessfulRecords(applicationId, workflowExecutionId, groupName, 0, 0, accountId);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testSaveMLAnalysisRecords() throws IOException {
    LearningEngineAnalysisTask analysisTask = LearningEngineAnalysisTask.builder().build();
    analysisTask.setCreatedAt(currentTimeMillis());
    String taskId = wingsPersistence.save(analysisTask);
    when(timeSeriesAnalysisService.saveAnalysisRecordsML(accountId, stateType, applicationId, stateExecutionId,
             workflowExecutionId, groupName, 0, taskId, baseLineExecutionId, cvConfigId, timeSeriesMLAnalysisRecord,
             null))
        .thenReturn(true);
    RestResponse<Boolean> resp = timeSeriesResource.saveMLAnalysisRecords(accountId, applicationId, stateType,
        stateExecutionId, workflowExecutionId, groupName, 0, taskId, baseLineExecutionId, cvConfigId, null,
        timeSeriesMLAnalysisRecord);
    assertThat(resp.getResource()).isTrue();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testGetScores() throws IOException {
    when(timeSeriesAnalysisService.getTimeSeriesMLScores(applicationId, workflowId, 0, 1))
        .thenReturn(timeSeriesMLScores);
    RestResponse<List<TimeSeriesMLScores>> resp =
        timeSeriesResource.getScores(accountId, applicationId, workflowId, 0, 1);
    assertThat(resp.getResource()).isEqualTo(timeSeriesMLScores);
  }

  @Test
  @Owner(developers = PRAVEEN)
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
    assertThat(resp.getResource()).isEqualTo(metricTemplate);
  }
}

package io.harness.jobs;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.VerificationBaseTest;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.TimeSeriesAnalysisService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Pranjal on 09/18/2018
 */
public class MetricAnalysisJobTest extends VerificationBaseTest {
  private String accountId;
  private String appId;
  private String stateExecutionId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;
  private String groupName;
  private String delegateTaskId;
  private String analysisServerConfigId;
  private String correlationId;
  private String preWorkflowExecutionId;
  private AnalysisContext analysisContext;
  MetricAnalysisJob metricAnalysisJob;

  @Mock VerificationManagerClient managerClient;
  @Mock LearningEngineService learningEngineService;
  @Mock TimeSeriesAnalysisService timeSeriesAnalysisService;
  @Mock JobExecutionContext jobExecutionContext;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    accountId = UUID.randomUUID().toString();
    appId = UUID.randomUUID().toString();
    stateExecutionId = UUID.randomUUID().toString();
    workflowId = UUID.randomUUID().toString();
    workflowExecutionId = UUID.randomUUID().toString();
    serviceId = UUID.randomUUID().toString();
    groupName = "groupName-";
    delegateTaskId = UUID.randomUUID().toString();
    analysisServerConfigId = UUID.randomUUID().toString();
    correlationId = UUID.randomUUID().toString();
    preWorkflowExecutionId = UUID.randomUUID().toString();

    analysisContext = AnalysisContext.builder()
                          .accountId(accountId)
                          .appId(appId)
                          .workflowId(workflowId)
                          .workflowExecutionId(workflowExecutionId)
                          .stateExecutionId(stateExecutionId)
                          .serviceId(serviceId)
                          .controlNodes(ImmutableMap.<String, String>builder()
                                            .put("control1", DEFAULT_GROUP_NAME)
                                            .put("control2", DEFAULT_GROUP_NAME)
                                            .build())
                          .testNodes(ImmutableMap.<String, String>builder()
                                         .put("test1", DEFAULT_GROUP_NAME)
                                         .put("test2", DEFAULT_GROUP_NAME)
                                         .build())
                          .isSSL(true)
                          .appPort(1234)
                          .comparisonStrategy(COMPARE_WITH_PREVIOUS)
                          .timeDuration(15)
                          .stateType(StateType.APP_DYNAMICS)
                          .analysisServerConfigId(analysisServerConfigId)
                          .correlationId(correlationId)
                          .prevWorkflowExecutionId(preWorkflowExecutionId)
                          .build();

    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("jobParams", JsonUtils.asJson(analysisContext));
    jobDataMap.put("delegateTaskId", delegateTaskId);
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);

    metricAnalysisJob = new MetricAnalysisJob(timeSeriesAnalysisService, learningEngineService, managerClient);

    setInternalState(metricAnalysisJob, "timeSeriesAnalysisService", timeSeriesAnalysisService);
    setInternalState(metricAnalysisJob, "learningEngineService", learningEngineService);
  }

  @Test
  public void testAnalysisJobQueuePreviousWithPredictive() throws Exception {
    Map<String, TimeSeriesMlAnalysisGroupInfo> metricGroups = new HashMap<>();
    metricGroups.put("tier3",
        TimeSeriesMlAnalysisGroupInfo.builder()
            .groupName("tier3")
            .mlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .dependencyPath("tier3->tier2->tier1")
            .build());

    metricGroups.put("tier2",
        TimeSeriesMlAnalysisGroupInfo.builder()
            .groupName("tier2")
            .mlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .dependencyPath("tier2->tier1")
            .build());

    metricGroups.put("tier1",
        TimeSeriesMlAnalysisGroupInfo.builder()
            .groupName("tier1")
            .mlAnalysisType(TimeSeriesMlAnalysisType.COMPARATIVE)
            .dependencyPath("tier1")
            .build());

    when(timeSeriesAnalysisService.isStateValid(appId, stateExecutionId)).thenReturn(true);
    when(timeSeriesAnalysisService.getMetricGroups(appId, stateExecutionId)).thenReturn(metricGroups);

    for (String groupName : timeSeriesAnalysisService.getMetricGroups(appId, stateExecutionId).keySet()) {
      when(timeSeriesAnalysisService.getAnalysisMinute(
               analysisContext.getStateType(), appId, stateExecutionId, workflowExecutionId, serviceId, groupName))
          .thenReturn(NewRelicMetricDataRecord.builder().dataCollectionMinute(10).build());

      when(timeSeriesAnalysisService.getMinControlMinuteWithData(analysisContext.getStateType(), appId, serviceId,
               workflowId, analysisContext.getPrevWorkflowExecutionId(), groupName))
          .thenReturn(2);

      when(timeSeriesAnalysisService.getMaxControlMinuteWithData(analysisContext.getStateType(), appId, serviceId,
               workflowId, analysisContext.getPrevWorkflowExecutionId(), groupName))
          .thenReturn(18);
    }

    metricAnalysisJob.execute(jobExecutionContext);

    verify(learningEngineService, times(3)).hasAnalysisTimedOut(anyString(), anyString(), anyString());
    verify(timeSeriesAnalysisService, times(3))
        .getMaxControlMinuteWithData(
            any(StateType.class), anyString(), anyString(), anyString(), anyString(), anyString());
  }
}
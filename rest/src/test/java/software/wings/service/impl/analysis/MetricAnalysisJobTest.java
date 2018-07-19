package software.wings.service.impl.analysis;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.service.impl.newrelic.MetricAnalysisJob;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MetricDataAnalysisService;
import software.wings.sm.StateType;
import software.wings.utils.JsonUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Created by rsingh on 5/20/18.
 */
public class MetricAnalysisJobTest extends WingsBaseTest {
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
  private MetricAnalysisJob metricAnalysisJob = new MetricAnalysisJob();

  @Inject private WingsPersistence wingsPersistence;
  @Inject private LearningEngineService learningEngineService;
  @Mock private MetricDataAnalysisService analysisService;
  @Mock JobExecutionContext jobExecutionContext;

  @Before
  public void setup() throws IOException {
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

    setInternalState(metricAnalysisJob, "analysisService", analysisService);
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
    when(analysisService.isStateValid(appId, stateExecutionId)).thenReturn(true);
    when(analysisService.getMetricGroups(appId, stateExecutionId)).thenReturn(metricGroups);

    for (String groupName : analysisService.getMetricGroups(appId, stateExecutionId).keySet()) {
      when(analysisService.getAnalysisMinute(
               analysisContext.getStateType(), appId, stateExecutionId, workflowExecutionId, serviceId, groupName))
          .thenReturn(NewRelicMetricDataRecord.builder().dataCollectionMinute(10).build());

      when(analysisService.getMinControlMinuteWithData(analysisContext.getStateType(), appId, serviceId, workflowId,
               analysisContext.getPrevWorkflowExecutionId(), groupName))
          .thenReturn(2);

      when(analysisService.getMaxControlMinuteWithData(analysisContext.getStateType(), appId, serviceId, workflowId,
               analysisContext.getPrevWorkflowExecutionId(), groupName))
          .thenReturn(18);
    }

    metricAnalysisJob.execute(jobExecutionContext);

    List<LearningEngineAnalysisTask> learningEngineAnalysisTasks =
        wingsPersistence.createQuery(LearningEngineAnalysisTask.class).asList();
    assertEquals(metricGroups.size(), learningEngineAnalysisTasks.size());

    int verified = 0;
    for (Entry<String, TimeSeriesMlAnalysisGroupInfo> entry : metricGroups.entrySet()) {
      for (LearningEngineAnalysisTask learningEngineAnalysisTask : learningEngineAnalysisTasks) {
        if (learningEngineAnalysisTask.getGroup_name().equals(entry.getKey())) {
          assertEquals(
              entry.getValue().getMlAnalysisType(), learningEngineAnalysisTask.getTime_series_ml_analysis_type());
          verified++;
        }
      }
    }
    assertEquals(metricGroups.size(), verified);
  }
}

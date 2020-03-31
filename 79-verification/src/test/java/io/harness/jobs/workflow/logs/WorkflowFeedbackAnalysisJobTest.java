package io.harness.jobs.workflow.logs;

import static io.harness.rule.OwnerRule.SOWMYA;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.VerificationBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.jobs.workflow.logs.WorkflowFeedbackAnalysisJob.FeedbackAnalysisTask;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.serializer.JsonUtils;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.intfc.DataStoreService;
import software.wings.verification.VerificationDataAnalysisResponse;

import java.io.IOException;
import java.util.List;

public class WorkflowFeedbackAnalysisJobTest extends VerificationBaseTest {
  @Mock private JobExecutionContext jobExecutionContext;
  @Mock private VerificationManagerClient verificationManagerClient;
  @Mock private LogAnalysisService analysisService;
  @Mock private LearningEngineService learningEngineService;
  @Inject private VerificationManagerClientHelper managerClientHelper;
  @Mock private DataStoreService dataStoreService;

  private WorkflowFeedbackAnalysisJob workflowFeedbackAnalysisJob;
  private AnalysisContext analysisContext;
  private FeedbackAnalysisTask feedbackAnalysisTask;
  private Call<RestResponse<Boolean>> managerCallFeedbacks;

  @Before
  public void setUp() throws IOException, IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    workflowFeedbackAnalysisJob = Mockito.spy(new WorkflowFeedbackAnalysisJob());
    managerCallFeedbacks = mock(Call.class);
    when(managerCallFeedbacks.clone()).thenReturn(managerCallFeedbacks);

    analysisContext = AnalysisContext.builder().build();
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put("jobParams", JsonUtils.asJson(analysisContext));
    JobDetail jobDetail = Mockito.mock(JobDetail.class);

    feedbackAnalysisTask = Mockito.mock(FeedbackAnalysisTask.class);

    FieldUtils.writeField(workflowFeedbackAnalysisJob, "managerClient", verificationManagerClient, true);
    FieldUtils.writeField(managerClientHelper, "managerClient", verificationManagerClient, true);
    FieldUtils.writeField(workflowFeedbackAnalysisJob, "managerClientHelper", managerClientHelper, true);
    FieldUtils.writeField(workflowFeedbackAnalysisJob, "analysisService", analysisService, true);
    FieldUtils.writeField(workflowFeedbackAnalysisJob, "learningEngineService", learningEngineService, true);
    FieldUtils.writeField(workflowFeedbackAnalysisJob, "dataStoreService", dataStoreService, true);

    Scheduler scheduler = Mockito.mock(Scheduler.class);
    when(jobExecutionContext.getMergedJobDataMap()).thenReturn(jobDataMap);
    when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
    when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
    when(managerCallFeedbacks.execute()).thenReturn(Response.success(new RestResponse<>(false)));
    when(verificationManagerClient.isFeatureEnabled(any(), any())).thenReturn(managerCallFeedbacks);
    when(learningEngineService.isStateValid(any(), any())).thenReturn(true);

    Call<RestResponse<Boolean>> notifyState = mock(Call.class);
    when(notifyState.clone()).thenReturn(notifyState);
    when(notifyState.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    when(verificationManagerClient.sendNotifyForVerificationState(
             anyMap(), anyString(), any(VerificationDataAnalysisResponse.class)))
        .thenReturn(notifyState);

    Call<RestResponse<List<String>>> versionsCall = mock(Call.class);
    when(versionsCall.clone()).thenReturn(versionsCall);
    when(versionsCall.execute()).thenReturn(Response.success(new RestResponse<>(Lists.newArrayList())));
    when(verificationManagerClient.getListOfPublishedVersions(anyString())).thenReturn(versionsCall);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testHandle_feedbackIterator() throws Exception {
    when(managerCallFeedbacks.execute()).thenReturn(Response.success(new RestResponse<>(true)));
    workflowFeedbackAnalysisJob.handle(analysisContext);
    verify(analysisService, times(2)).getLastWorkflowAnalysisMinute(any(), any(), any());
  }
}
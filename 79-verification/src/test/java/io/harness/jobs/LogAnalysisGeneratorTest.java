package io.harness.jobs;

import static io.harness.rule.OwnerRule.NANDAN;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.service.impl.analysis.AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.MLAnalysisType;
import software.wings.service.impl.newrelic.LearningEngineAnalysisTask;
import software.wings.sm.StateType;

import java.util.UUID;

public class LogAnalysisGeneratorTest extends CategoryTest {
  private String accountId;
  private String applicationId;
  private String workflowId;
  private String serviceId;
  private int logAnalysisMinute;
  private String stateExecutionId;
  private String workflowExecutionId;
  private String groupName;
  private String delegateTaskId;
  private String analysisServerConfigId;
  private String correlationId;
  private String preWorkflowExecutionId;
  @Mock private LogAnalysisService analysisService;
  @Mock private LearningEngineService learningEngineService;
  @Mock private VerificationManagerClient managerClient;
  @Mock private VerificationManagerClientHelper managerClientHelper;
  AnalysisContext analysisContext;
  LogMLAnalysisGenerator logMLAnalysisGenerator;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    accountId = UUID.randomUUID().toString();
    applicationId = UUID.randomUUID().toString();
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
                          .appId(applicationId)
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
                          .query("TestQuery")
                          .prevWorkflowExecutionId(preWorkflowExecutionId)
                          .build();

    logMLAnalysisGenerator = new LogMLAnalysisGenerator(analysisContext, logAnalysisMinute, false, analysisService,
        learningEngineService, managerClient, managerClientHelper, null);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testBugsnagNonNN() {
    analysisContext.setStateType(StateType.BUG_SNAG);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(new RestResponse<Boolean>(false));
    logMLAnalysisGenerator = new LogMLAnalysisGenerator(analysisContext, logAnalysisMinute, false, analysisService,
        learningEngineService, managerClient, managerClientHelper, null);
    logMLAnalysisGenerator.run();
    ArgumentCaptor<LearningEngineAnalysisTask> taskCaptor = ArgumentCaptor.forClass(LearningEngineAnalysisTask.class);

    verify(learningEngineService).addLearningEngineAnalysisTask(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getFeature_name()).isNull();
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testNonBugsnagNN() {
    analysisContext.setStateType(StateType.APP_DYNAMICS);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(new RestResponse<Boolean>(false));
    logMLAnalysisGenerator = new LogMLAnalysisGenerator(analysisContext, logAnalysisMinute, false, analysisService,
        learningEngineService, managerClient, managerClientHelper, null);
    logMLAnalysisGenerator.run();
    ArgumentCaptor<LearningEngineAnalysisTask> taskCaptor = ArgumentCaptor.forClass(LearningEngineAnalysisTask.class);

    verify(learningEngineService).addLearningEngineAnalysisTask(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getFeature_name()).isNotNull();
  }

  @Test
  @Owner(developers = NANDAN)
  @Category(UnitTests.class)
  public void testFeedbackAnalsyisRecordIs24x7FlagFalse() {
    analysisContext.setStateType(StateType.APP_DYNAMICS);
    when(managerClientHelper.callManagerWithRetry(any())).thenReturn(new RestResponse<Boolean>(false));
    logMLAnalysisGenerator = new LogMLAnalysisGenerator(analysisContext, logAnalysisMinute, false, analysisService,
        learningEngineService, managerClient, managerClientHelper, MLAnalysisType.FEEDBACK_ANALYSIS);
    logMLAnalysisGenerator.run();
    ArgumentCaptor<LearningEngineAnalysisTask> taskCaptor = ArgumentCaptor.forClass(LearningEngineAnalysisTask.class);
    verify(learningEngineService).addLearningEngineAnalysisTask(taskCaptor.capture());
    assertThat(taskCaptor.getValue().is24x7Task()).isEqualTo(Boolean.FALSE);
  }
}

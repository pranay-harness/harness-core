package io.harness.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.VerificationBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.VerificationManagerClient;
import io.harness.managerclient.VerificationManagerClientHelper;
import io.harness.network.SafeHttpCall;
import io.harness.resources.ExperimentalLogAnalysisResourceImpl;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.LearningEngineService;
import io.harness.service.intfc.LogAnalysisService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.WorkflowExecution;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.ExperimentalLogMLAnalysisRecord;
import software.wings.service.impl.analysis.LogMLAnalysisSummary;
import software.wings.service.impl.analysis.LogMLExpAnalysisInfo;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by Pranjal on 09/24/2018
 */
public class ExperimentalLogAnalysisResourceImplTest extends VerificationBaseTest {
  private ExperimentalLogAnalysisResourceImpl logAnalysisResource;

  private LogAnalysisService logAnalysisService;
  private LearningEngineService learningEngineService;
  private VerificationManagerClient verificationManagerClient;
  private VerificationManagerClientHelper managerClientHelper;
  private SafeHttpCall httpCall;
  private WorkflowExecution workflowExecution;

  String mockAccountId = String.valueOf(Math.random());
  String mockApplicationId = String.valueOf(Math.random());

  String mockStateExecutionId = String.valueOf(Math.random());
  Integer logCollectionMinute = 15;

  String taskId = String.valueOf(Math.random());
  private String workFlowExecutionId = String.valueOf(Math.random());
  private ExperimentalLogMLAnalysisRecord experimentalLogMLAnalysisRecord;

  @Before
  public void setUp() throws IOException {
    logAnalysisService = mock(LogAnalysisService.class);
    learningEngineService = mock(LearningEngineService.class);
    verificationManagerClient = mock(VerificationManagerClient.class);
    managerClientHelper = new VerificationManagerClientHelper();
    logAnalysisResource = new ExperimentalLogAnalysisResourceImpl(
        logAnalysisService, learningEngineService, verificationManagerClient, managerClientHelper);
    when(logAnalysisService.reQueueExperimentalTask(anyString(), anyString())).thenReturn(true);
  }

  @Test
  @Category(UnitTests.class)
  public void testSaveLogAnalysisMLRecords_shouldSuccess() throws IOException {
    Call<RestResponse<WorkflowExecution>> restCall = mock(Call.class);
    when(restCall.execute()).thenReturn(Response.success(new RestResponse<>(WorkflowExecution.builder().build())));
    when(verificationManagerClient.getWorkflowExecution(anyString(), anyString())).thenReturn(restCall);
    when(logAnalysisService.saveExperimentalLogAnalysisRecords(
             any(ExperimentalLogMLAnalysisRecord.class), any(StateType.class), any(Optional.class)))
        .thenReturn(true);
    RestResponse<Boolean> response = logAnalysisResource.saveLogAnalysisMLRecords(mockAccountId, mockApplicationId,
        mockStateExecutionId, logCollectionMinute, false, taskId, StateType.ELK, getMLAnalysisRecord());

    assertTrue(response.getResource());
  }

  @Test
  @Category(UnitTests.class)
  public void testGetLogExpAnalysisInfo_shouldSuccess() throws IOException {
    List<LogMLExpAnalysisInfo> expectedData = getLogMLExpAnalysisInfo();
    when(logAnalysisService.getExpAnalysisInfoList()).thenReturn(expectedData);
    RestResponse<List<LogMLExpAnalysisInfo>> response = logAnalysisResource.getLogExpAnalysisInfo(mockAccountId);

    assertEquals(response.getResource(), expectedData);
  }

  @Test
  @Category(UnitTests.class)
  public void testExperimentalTask_shouldSuccess() throws IOException {
    RestResponse<Boolean> response =
        logAnalysisResource.experimentalTask(mockAccountId, mockApplicationId, mockStateExecutionId);

    assertTrue(response.getResource());
  }

  private ExperimentalLogMLAnalysisRecord getMLAnalysisRecord() {
    return new ExperimentalLogMLAnalysisRecord();
  }

  private LogMLAnalysisSummary getLogMLAnalysisSummary() {
    LogMLAnalysisSummary summary = LogMLAnalysisSummary.builder().build();
    summary.setQuery("exception");
    summary.setRiskLevel(RiskLevel.LOW);
    return summary;
  }

  private List<LogMLExpAnalysisInfo> getLogMLExpAnalysisInfo() {
    List<LogMLExpAnalysisInfo> logMLExpAnalysisInfoList = new ArrayList<>();
    LogMLExpAnalysisInfo testdata1 = LogMLExpAnalysisInfo.builder()
                                         .stateExecutionId(mockStateExecutionId)
                                         .appId(mockApplicationId)
                                         .stateType(StateType.ELK)
                                         .expName("testExp")
                                         .workflowExecutionId(workFlowExecutionId)
                                         .build();
    logMLExpAnalysisInfoList.add(testdata1);
    return logMLExpAnalysisInfoList;
  }
}

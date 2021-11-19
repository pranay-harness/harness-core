package io.harness.delegate.task.citasks.awsvm;

import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.awsvm.AwsVmTaskExecutionResponse;
import io.harness.delegate.beans.ci.awsvm.CIAWSVmExecuteStepTaskParams;
import io.harness.delegate.beans.ci.awsvm.runner.ExecuteStepResponse;
import io.harness.delegate.task.citasks.awsvm.helper.HttpHelper;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIAwsVmExecuteStepTaskHandlerTest extends CategoryTest {
  @Mock private HttpHelper httpHelper;
  @InjectMocks private CIAwsVmExecuteStepTaskHandler ciAwsVmExecuteStepTaskHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternal() throws IOException {
    CIAWSVmExecuteStepTaskParams params = CIAWSVmExecuteStepTaskParams.builder().stageRuntimeId("stage").build();
    Response<ExecuteStepResponse> executeStepResponse =
        Response.success(ExecuteStepResponse.builder().ExitCode(0).build());
    when(httpHelper.executeStepWithRetries(anyMap())).thenReturn(executeStepResponse);
    AwsVmTaskExecutionResponse response = ciAwsVmExecuteStepTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalFailure() {
    CIAWSVmExecuteStepTaskParams params = CIAWSVmExecuteStepTaskParams.builder().stageRuntimeId("stage").build();
    Response<ExecuteStepResponse> executeStepResponse =
        Response.success(ExecuteStepResponse.builder().ExitCode(1).build());
    when(httpHelper.executeStepWithRetries(anyMap())).thenReturn(executeStepResponse);
    AwsVmTaskExecutionResponse response = ciAwsVmExecuteStepTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }
}

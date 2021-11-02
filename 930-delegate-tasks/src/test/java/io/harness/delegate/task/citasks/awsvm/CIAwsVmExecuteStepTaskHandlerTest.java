package io.harness.delegate.task.citasks.awsvm;

import static io.harness.delegate.task.citasks.awsvm.helper.CIAwsVmConstants.RUNNER_EXECUTE_STEP_URL;
import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.awsvm.AwsVmTaskExecutionResponse;
import io.harness.delegate.beans.ci.awsvm.CIAWSVmExecuteStepTaskParams;
import io.harness.delegate.task.citasks.awsvm.helper.HttpHelper;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    String body = "{\"ExitCode\": 0}";
    Response httpResponse = new Response.Builder()
                                .protocol(Protocol.HTTP_1_1)
                                .request(new Request.Builder().url("http://localhost").build())
                                .code(200) // status code
                                .message("")
                                .body(ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), body))
                                .build();
    when(httpHelper.post(eq(RUNNER_EXECUTE_STEP_URL), anyString(), eq(14400))).thenReturn(httpResponse);
    AwsVmTaskExecutionResponse response = ciAwsVmExecuteStepTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalFailure() throws IOException {
    CIAWSVmExecuteStepTaskParams params = CIAWSVmExecuteStepTaskParams.builder().stageRuntimeId("stage").build();
    when(httpHelper.post(eq(RUNNER_EXECUTE_STEP_URL), anyString(), eq(14400))).thenReturn(null);
    AwsVmTaskExecutionResponse response = ciAwsVmExecuteStepTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }
}

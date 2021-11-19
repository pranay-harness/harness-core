package io.harness.delegate.task.citasks.awsvm;

import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.awsvm.AwsVmTaskExecutionResponse;
import io.harness.delegate.beans.ci.awsvm.CIAWSVmInitializeTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.citasks.awsvm.helper.HttpHelper;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIAwsVmInitializeTaskHandlerTest extends CategoryTest {
  @Mock private HttpHelper httpHelper;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;
  @InjectMocks private CIAwsVmInitializeTaskHandler ciAwsVmInitializeTaskHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternal() throws IOException {
    CIAWSVmInitializeTaskParams params = CIAWSVmInitializeTaskParams.builder().stageRuntimeId("stage").build();
    Response<Void> setupResponse = Response.success(null);
    when(httpHelper.setupStageWithRetries(anyMap())).thenReturn(setupResponse);
    AwsVmTaskExecutionResponse response =
        ciAwsVmInitializeTaskHandler.executeTaskInternal(params, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalFailure() throws IOException {
    CIAWSVmInitializeTaskParams params = CIAWSVmInitializeTaskParams.builder().stageRuntimeId("stage").build();
    ResponseBody body = mock(ResponseBody.class);
    Response<Void> setupResponse = Response.error(400, body);
    when(httpHelper.setupStageWithRetries(anyMap())).thenReturn(setupResponse);
    AwsVmTaskExecutionResponse response =
        ciAwsVmInitializeTaskHandler.executeTaskInternal(params, logStreamingTaskClient);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }
}
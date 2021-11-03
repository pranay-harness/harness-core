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
import io.harness.delegate.beans.ci.awsvm.CIAwsVmCleanupTaskParams;
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
public class CIAwsVmCleanupTaskHandlerTest extends CategoryTest {
  @Mock private HttpHelper httpHelper;
  @InjectMocks private CIAwsVmCleanupTaskHandler ciAwsVmCleanupTaskHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternal() throws IOException {
    CIAwsVmCleanupTaskParams params = CIAwsVmCleanupTaskParams.builder().stageRuntimeId("stage").build();
    Response<Void> cleanupResponse = Response.success(null);
    when(httpHelper.cleanupStageWithRetries(anyMap())).thenReturn(cleanupResponse);
    AwsVmTaskExecutionResponse response = ciAwsVmCleanupTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.SUCCESS, response.getCommandExecutionStatus());
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalFailure() {
    CIAwsVmCleanupTaskParams params = CIAwsVmCleanupTaskParams.builder().stageRuntimeId("stage").build();
    ResponseBody body = mock(ResponseBody.class);
    Response<Void> cleanupResponse = Response.error(400, body);
    when(httpHelper.cleanupStageWithRetries(anyMap())).thenReturn(cleanupResponse);
    AwsVmTaskExecutionResponse response = ciAwsVmCleanupTaskHandler.executeTaskInternal(params);
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }
}

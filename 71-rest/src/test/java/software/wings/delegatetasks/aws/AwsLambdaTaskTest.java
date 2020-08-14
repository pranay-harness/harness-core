package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;
import software.wings.service.impl.aws.model.AwsLambdaFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

public class AwsLambdaTaskTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private AwsLambdaHelperServiceDelegate mockAwsLambdaHelperServiceDelegate;

  @InjectMocks
  private AwsLambdaTask task = (AwsLambdaTask) TaskType.AWS_LAMBDA_TASK.getDelegateRunnableTask(
      DelegateTaskPackage.builder()
          .delegateId("delegateid")
          .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
          .build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsLambdaHelperServiceDelegate", mockAwsLambdaHelperServiceDelegate);
    on(task).set("delegateLogService", mockDelegateLogService);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteWf() {
    AwsLambdaRequest request = AwsLambdaExecuteWfRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsLambdaHelperServiceDelegate).executeWf(any(), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteFunction() {
    AwsLambdaRequest request = AwsLambdaExecuteFunctionRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsLambdaHelperServiceDelegate).executeFunction(any());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteFunctionWithException() {
    AwsLambdaExecuteFunctionRequest request = AwsLambdaExecuteFunctionRequest.builder().build();
    doThrow(new RuntimeException("Error msg")).when(mockAwsLambdaHelperServiceDelegate).executeFunction(request);

    AwsResponse awsResponse = task.run(new Object[] {request});

    verify(mockAwsLambdaHelperServiceDelegate).executeFunction(any());
    assertThat(awsResponse instanceof AwsLambdaExecuteFunctionResponse).isTrue();
    assertThat(((AwsLambdaExecuteFunctionResponse) awsResponse).getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(((AwsLambdaExecuteFunctionResponse) awsResponse).getErrorMessage())
        .isEqualTo("RuntimeException: Error msg");
    assertThatExceptionOfType(Exception.class)
        .isThrownBy(() -> mockAwsLambdaHelperServiceDelegate.executeFunction(request));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteWfWithException() {
    AwsLambdaExecuteWfRequest request = AwsLambdaExecuteWfRequest.builder().build();
    doThrow(new RuntimeException("Error msg")).when(mockAwsLambdaHelperServiceDelegate).executeWf(any(), any());

    AwsResponse awsResponse = task.run(new Object[] {request});

    verify(mockAwsLambdaHelperServiceDelegate).executeWf(any(), any());
    assertThat(awsResponse instanceof AwsLambdaExecuteWfResponse).isTrue();
    assertThat(((AwsLambdaExecuteWfResponse) awsResponse).getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(((AwsLambdaExecuteWfResponse) awsResponse).getErrorMessage()).isEqualTo("RuntimeException: Error msg");
    assertThatExceptionOfType(Exception.class)
        .isThrownBy(() -> mockAwsLambdaHelperServiceDelegate.executeWf(any(), any()));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetLambdaFunctions() {
    AwsLambdaFunctionRequest request = AwsLambdaFunctionRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsLambdaHelperServiceDelegate).getLambdaFunctions(request);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetLambdaFunctionsWithException() {
    AwsLambdaFunctionRequest request = AwsLambdaFunctionRequest.builder().build();
    doThrow(new RuntimeException("Error msg")).when(mockAwsLambdaHelperServiceDelegate).getLambdaFunctions(request);

    AwsResponse awsResponse = task.run(new Object[] {request});

    verify(mockAwsLambdaHelperServiceDelegate).getLambdaFunctions(any());
    assertThat(awsResponse instanceof AwsLambdaFunctionResponse).isTrue();
    assertThat(((AwsLambdaFunctionResponse) awsResponse).getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(((AwsLambdaFunctionResponse) awsResponse).getErrorMessage()).isEqualTo("RuntimeException: Error msg");
    assertThatExceptionOfType(Exception.class)
        .isThrownBy(() -> mockAwsLambdaHelperServiceDelegate.getLambdaFunctions(request));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetFunctionDetails() {
    AwsLambdaDetailsRequest request = AwsLambdaDetailsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsLambdaHelperServiceDelegate).getFunctionDetails(request);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testGetFunctionDetailsWithException() {
    AwsLambdaDetailsRequest request = AwsLambdaDetailsRequest.builder().build();
    doThrow(new RuntimeException("Error msg")).when(mockAwsLambdaHelperServiceDelegate).getFunctionDetails(request);

    AwsResponse awsResponse = task.run(new Object[] {request});

    verify(mockAwsLambdaHelperServiceDelegate).getFunctionDetails(any());
    assertThat(awsResponse instanceof AwsLambdaFunctionResponse).isTrue();
    assertThat(((AwsLambdaFunctionResponse) awsResponse).getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(((AwsLambdaFunctionResponse) awsResponse).getErrorMessage()).isEqualTo("RuntimeException: Error msg");
    assertThatExceptionOfType(Exception.class)
        .isThrownBy(() -> mockAwsLambdaHelperServiceDelegate.getFunctionDetails(request));
  }
}

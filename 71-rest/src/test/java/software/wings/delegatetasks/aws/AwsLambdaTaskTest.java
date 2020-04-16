package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
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
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

public class AwsLambdaTaskTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private AwsLambdaHelperServiceDelegate mockAwsLambdaHelperServiceDelegate;

  @InjectMocks
  private AwsLambdaTask task = (AwsLambdaTask) TaskType.AWS_LAMBDA_TASK.getDelegateRunnableTask("delegateid",
      DelegateTask.builder().data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
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
}
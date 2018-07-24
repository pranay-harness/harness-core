package software.wings.delegatetasks.aws;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaRequest;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

public class AwsLambdaAsyncTaskTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private AwsLambdaHelperServiceDelegate mockAwsLambdaHelperServiceDelegate;

  @InjectMocks
  private AwsLambdaAsyncTask task = (AwsLambdaAsyncTask) TaskType.AWS_LAMBDA_ASYNC_TASK.getDelegateRunnableTask(
      "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsLambdaHelperServiceDelegate", mockAwsLambdaHelperServiceDelegate);
    on(task).set("delegateLogService", mockDelegateLogService);
  }

  @Test
  public void testRun() {
    AwsLambdaRequest request = AwsLambdaExecuteWfRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsLambdaHelperServiceDelegate).executeWf(any(), any());
  }
}
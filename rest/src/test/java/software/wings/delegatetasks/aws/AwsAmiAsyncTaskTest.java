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
import software.wings.service.impl.aws.model.AwsAmiServiceSetupRequest;
import software.wings.service.intfc.aws.delegate.AwsAmiHelperServiceDelegate;

public class AwsAmiAsyncTaskTest extends WingsBaseTest {
  @Mock private DelegateLogService mockDelegateLogService;
  @Mock private AwsAmiHelperServiceDelegate mockAwsAmiHelperServiceDelegate;

  @InjectMocks
  private AwsAmiAsyncTask task = (AwsAmiAsyncTask) TaskType.AWS_AMI_ASYNC_TASK.getDelegateRunnableTask(
      "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("delegateLogService", mockDelegateLogService);
    on(task).set("awsAmiHelperServiceDelegate", mockAwsAmiHelperServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsAmiServiceSetupRequest request = AwsAmiServiceSetupRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsAmiHelperServiceDelegate).setUpAmiService(any(), any());
  }
}
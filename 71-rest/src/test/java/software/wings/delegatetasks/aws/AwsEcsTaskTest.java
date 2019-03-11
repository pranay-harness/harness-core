package software.wings.delegatetasks.aws;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.delegate.beans.TaskData;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsEcsListClustersRequest;
import software.wings.service.impl.aws.model.AwsEcsRequest;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

public class AwsEcsTaskTest extends WingsBaseTest {
  @Mock private AwsEcsHelperServiceDelegate mockEcsHelperServiceDelegate;

  @InjectMocks
  private AwsEcsTask task = (AwsEcsTask) TaskType.AWS_ECS_TASK.getDelegateRunnableTask("delegateid",
      DelegateTask.builder().async(true).data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("ecsHelperServiceDelegate", mockEcsHelperServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsEcsRequest request = AwsEcsListClustersRequest.builder().build();
    task.run(request);
    verify(mockEcsHelperServiceDelegate).listClusters(any(), anyList(), anyString());
  }
}
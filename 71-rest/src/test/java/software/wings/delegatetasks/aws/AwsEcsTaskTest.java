package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
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
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testRun() {
    AwsEcsRequest request = AwsEcsListClustersRequest.builder().build();
    task.run(request);
    verify(mockEcsHelperServiceDelegate).listClusters(any(), anyList(), anyString());
  }
}
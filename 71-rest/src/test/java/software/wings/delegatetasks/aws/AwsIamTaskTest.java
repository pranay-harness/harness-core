package software.wings.delegatetasks.aws;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListRolesRequest;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;

public class AwsIamTaskTest extends WingsBaseTest {
  @Mock private AwsIamHelperServiceDelegate mockIamServiceDelegate;

  @InjectMocks
  private AwsIamTask task = (AwsIamTask) TaskType.AWS_IAM_TASK.getDelegateRunnableTask("delegateid",
      DelegateTask.builder().async(true).data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("iAmServiceDelegate", mockIamServiceDelegate);
  }

  @Test
  @Category(UnitTests.class)
  public void testRun() {
    AwsIamRequest request = AwsIamListRolesRequest.builder().build();
    task.run(request);
    verify(mockIamServiceDelegate).listIAMRoles(any(), anyList());
    request = AwsIamListInstanceRolesRequest.builder().build();
    task.run(request);
    verify(mockIamServiceDelegate).listIamInstanceRoles(any(), anyList());
  }
}
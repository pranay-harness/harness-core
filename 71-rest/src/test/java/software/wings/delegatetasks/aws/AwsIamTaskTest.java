package software.wings.delegatetasks.aws;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListRolesRequest;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;

public class AwsIamTaskTest extends WingsBaseTest {
  @Mock private AwsIamHelperServiceDelegate mockIamServiceDelegate;

  @InjectMocks
  private AwsIamTask task = (AwsIamTask) TaskType.AWS_IAM_TASK.getDelegateRunnableTask(
      "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("iAmServiceDelegate", mockIamServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsIamRequest request = AwsIamListRolesRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockIamServiceDelegate).listIAMRoles(any(), anyList());
    request = AwsIamListInstanceRolesRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockIamServiceDelegate).listIamInstanceRoles(any(), anyList());
  }
}
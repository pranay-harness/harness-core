package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesRequest;
import software.wings.service.impl.aws.model.AwsIamListRolesRequest;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class AwsIamTaskTest extends WingsBaseTest {
  @Mock private AwsIamHelperServiceDelegate mockIamServiceDelegate;

  @InjectMocks
  private AwsIamTask task =
      new AwsIamTask(DelegateTaskPackage.builder()
                         .delegateId("delegateid")
                         .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
                         .build(),
          null, notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("iAmServiceDelegate", mockIamServiceDelegate);
  }

  @Test
  @Owner(developers = SATYAM)
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

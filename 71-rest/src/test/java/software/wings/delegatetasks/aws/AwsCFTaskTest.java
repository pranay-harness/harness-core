package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
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
import software.wings.service.impl.aws.model.AwsCFGetTemplateParamsRequest;
import software.wings.service.impl.aws.model.AwsCFRequest;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

public class AwsCFTaskTest extends WingsBaseTest {
  @Mock private AwsCFHelperServiceDelegate mockAwsCFHelperServiceDelegate;

  @InjectMocks
  private AwsCFTask task = (AwsCFTask) TaskType.AWS_CF_TASK.getDelegateRunnableTask("delegateid",
      DelegateTask.builder().data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRun() {
    AwsCFRequest request = AwsCFGetTemplateParamsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsCFHelperServiceDelegate)
        .getParamsData(any(), anyList(), anyString(), anyString(), anyString(), anyObject(), anyObject(), anyList());
  }
}
package software.wings.delegatetasks.aws;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import org.junit.Before;
import org.junit.Test;
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
  private AwsCFTask task = (AwsCFTask) TaskType.AWS_CF_TASK.getDelegateRunnableTask(
      "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsCFHelperServiceDelegate", mockAwsCFHelperServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsCFRequest request = AwsCFGetTemplateParamsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsCFHelperServiceDelegate).getParamsData(any(), anyList(), anyString(), anyString(), anyString());
  }
}
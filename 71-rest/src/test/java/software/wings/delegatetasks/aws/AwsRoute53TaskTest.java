package software.wings.delegatetasks.aws;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesRequest;
import software.wings.service.impl.aws.model.AwsRoute53Request;
import software.wings.service.intfc.aws.delegate.AwsRoute53HelperServiceDelegate;

public class AwsRoute53TaskTest extends WingsBaseTest {
  @Mock private AwsRoute53HelperServiceDelegate mockAwsRoute53HelperServiceDelegate;

  @InjectMocks
  private AwsRoute53Task task = (AwsRoute53Task) TaskType.AWS_ROUTE53_TASK.getDelegateRunnableTask("delegateid",
      aDelegateTask().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsRoute53HelperServiceDelegate", mockAwsRoute53HelperServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsRoute53Request request = AwsRoute53ListHostedZonesRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsRoute53HelperServiceDelegate).listHostedZones(any(), anyList(), anyString());
  }
}
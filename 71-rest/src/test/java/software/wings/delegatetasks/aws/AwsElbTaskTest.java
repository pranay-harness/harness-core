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
import software.wings.service.impl.aws.model.AwsElbListAppElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListNetworkElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsRequest;
import software.wings.service.impl.aws.model.AwsElbRequest;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

public class AwsElbTaskTest extends WingsBaseTest {
  @Mock private AwsElbHelperServiceDelegate mockElbHelperServiceDelegate;

  @InjectMocks
  private AwsElbTask task = (AwsElbTask) TaskType.AWS_ELB_TASK.getDelegateRunnableTask(
      "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("elbHelperServiceDelegate", mockElbHelperServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsElbRequest request = AwsElbListClassicElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listClassicLoadBalancers(any(), anyList(), anyString());
    request = AwsElbListAppElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listApplicationLoadBalancers(any(), anyList(), anyString());
    request = AwsElbListTargetGroupsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listTargetGroupsForAlb(any(), anyList(), anyString(), anyString());
    request = AwsElbListElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listElasticLoadBalancers(any(), anyList(), anyString());
    request = AwsElbListNetworkElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listNetworkLoadBalancers(any(), anyList(), anyString());
  }
}
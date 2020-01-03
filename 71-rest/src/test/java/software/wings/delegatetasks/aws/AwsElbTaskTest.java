package software.wings.delegatetasks.aws;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
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
import software.wings.service.impl.aws.model.AwsElbListAppElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListClassicElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListListenerRequest;
import software.wings.service.impl.aws.model.AwsElbListNetworkElbsRequest;
import software.wings.service.impl.aws.model.AwsElbListTargetGroupsRequest;
import software.wings.service.impl.aws.model.AwsElbRequest;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

public class AwsElbTaskTest extends WingsBaseTest {
  @Mock private AwsElbHelperServiceDelegate mockElbHelperServiceDelegate;

  @InjectMocks
  private AwsElbTask task = (AwsElbTask) TaskType.AWS_ELB_TASK.getDelegateRunnableTask("delegateid",
      DelegateTask.builder().async(true).data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("elbHelperServiceDelegate", mockElbHelperServiceDelegate);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRun() {
    AwsElbRequest request = AwsElbListClassicElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listClassicLoadBalancers(any(), anyList(), anyString());
    request = AwsElbListAppElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listApplicationLoadBalancerDetails(any(), anyList(), anyString());
    request = AwsElbListTargetGroupsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listTargetGroupsForAlb(any(), anyList(), anyString(), anyString());
    request = AwsElbListElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listElasticLoadBalancerDetails(any(), anyList(), anyString());
    request = AwsElbListNetworkElbsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).listNetworkLoadBalancerDetails(any(), anyList(), anyString());
    request = AwsElbListListenerRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockElbHelperServiceDelegate).getElbListenersForLoadBalaner(any(), anyList(), anyString(), anyString());
  }
}
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
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountRequest;
import software.wings.service.impl.aws.model.AwsAsgListAllNamesRequest;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesRequest;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.impl.aws.model.AwsAsgRequest;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;

public class AwsAsgTaskTest extends WingsBaseTest {
  @Mock private AwsAsgHelperServiceDelegate mockAwsAsgHelperServiceDelegate;

  @InjectMocks
  private AwsAsgTask task = (AwsAsgTask) TaskType.AWS_ASG_TASK.getDelegateRunnableTask("delegateid",
      DelegateTask.builder().data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsAsgHelperServiceDelegate", mockAwsAsgHelperServiceDelegate);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRun() {
    AwsAsgRequest request = AwsAsgListAllNamesRequest.builder().build();
    task.run(request);
    verify(mockAwsAsgHelperServiceDelegate).listAutoScalingGroupNames(any(), anyList(), anyString());
    request = AwsAsgListInstancesRequest.builder().build();
    task.run(request);
    verify(mockAwsAsgHelperServiceDelegate).listAutoScalingGroupInstances(any(), anyList(), anyString(), anyString());
    request = AwsAsgListDesiredCapacitiesRequest.builder().build();
    task.run(request);
    verify(mockAwsAsgHelperServiceDelegate).getDesiredCapacitiesOfAsgs(any(), anyList(), anyString(), anyList());
    request = AwsAsgGetRunningCountRequest.builder().build();
    task.run(request);
    verify(mockAwsAsgHelperServiceDelegate)
        .getCurrentlyRunningInstanceCount(any(), anyList(), anyString(), anyString());
  }
}
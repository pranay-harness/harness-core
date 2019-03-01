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
import software.wings.service.impl.aws.model.AwsAsgListAllNamesRequest;
import software.wings.service.impl.aws.model.AwsAsgListDesiredCapacitiesRequest;
import software.wings.service.impl.aws.model.AwsAsgListInstancesRequest;
import software.wings.service.impl.aws.model.AwsAsgRequest;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;

public class AwsAsgTaskTest extends WingsBaseTest {
  @Mock private AwsAsgHelperServiceDelegate mockAwsAsgHelperServiceDelegate;

  @InjectMocks
  private AwsAsgTask task = (AwsAsgTask) TaskType.AWS_ASG_TASK.getDelegateRunnableTask("delegateid",
      aDelegateTask().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsAsgHelperServiceDelegate", mockAwsAsgHelperServiceDelegate);
  }

  @Test
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
  }
}
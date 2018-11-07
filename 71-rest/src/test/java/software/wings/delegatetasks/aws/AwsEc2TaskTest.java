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
import software.wings.service.impl.aws.model.AwsEc2ListInstancesRequest;
import software.wings.service.impl.aws.model.AwsEc2ListRegionsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSGsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListSubnetsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListTagsRequest;
import software.wings.service.impl.aws.model.AwsEc2ListVpcsRequest;
import software.wings.service.impl.aws.model.AwsEc2Request;
import software.wings.service.impl.aws.model.AwsEc2ValidateCredentialsRequest;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

public class AwsEc2TaskTest extends WingsBaseTest {
  @Mock private AwsEc2HelperServiceDelegate mockEc2ServiceDelegate;

  @InjectMocks
  private AwsEc2Task task = (AwsEc2Task) TaskType.AWS_EC2_TASK.getDelegateRunnableTask(
      "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("ec2ServiceDelegate", mockEc2ServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsEc2Request request = AwsEc2ValidateCredentialsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEc2ServiceDelegate).validateAwsAccountCredential(any(), anyList());
    request = AwsEc2ListRegionsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEc2ServiceDelegate).listRegions(any(), anyList());
    request = AwsEc2ListVpcsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEc2ServiceDelegate).listVPCs(any(), anyList(), anyString());
    request = AwsEc2ListSubnetsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEc2ServiceDelegate).listSubnets(any(), anyList(), anyString(), anyList());
    request = AwsEc2ListSGsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEc2ServiceDelegate).listSGs(any(), anyList(), anyString(), anyList());
    request = AwsEc2ListTagsRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEc2ServiceDelegate).listTags(any(), anyList(), anyString(), anyString());
    request = AwsEc2ListInstancesRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockEc2ServiceDelegate).listEc2Instances(any(), anyList(), anyString(), anyList());
  }
}
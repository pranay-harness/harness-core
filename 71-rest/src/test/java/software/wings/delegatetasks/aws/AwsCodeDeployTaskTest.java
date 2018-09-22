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
import software.wings.service.impl.aws.model.AwsCodeDeployListAppRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListAppRevisionRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentConfigRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentGroupRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentInstancesRequest;
import software.wings.service.impl.aws.model.AwsCodeDeployRequest;
import software.wings.service.intfc.aws.delegate.AwsCodeDeployHelperServiceDelegate;

public class AwsCodeDeployTaskTest extends WingsBaseTest {
  @Mock private AwsCodeDeployHelperServiceDelegate mockAwsCodeDeployHelperServiceDelegate;

  @InjectMocks
  private AwsCodeDeployTask task = (AwsCodeDeployTask) TaskType.AWS_CODE_DEPLOY_TASK.getDelegateRunnableTask(
      "delegateid", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("awsCodeDeployHelperServiceDelegate", mockAwsCodeDeployHelperServiceDelegate);
  }

  @Test
  public void testRun() {
    AwsCodeDeployRequest request = AwsCodeDeployListAppRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsCodeDeployHelperServiceDelegate).listApplications(any(), anyList(), anyString());
    request = AwsCodeDeployListDeploymentConfigRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsCodeDeployHelperServiceDelegate).listDeploymentConfiguration(any(), anyList(), anyString());
    request = AwsCodeDeployListDeploymentGroupRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsCodeDeployHelperServiceDelegate).listDeploymentGroups(any(), anyList(), anyString(), anyString());
    request = AwsCodeDeployListDeploymentInstancesRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsCodeDeployHelperServiceDelegate).listDeploymentInstances(any(), anyList(), anyString(), anyString());
    request = AwsCodeDeployListAppRevisionRequest.builder().build();
    task.run(new Object[] {request});
    verify(mockAwsCodeDeployHelperServiceDelegate)
        .listAppRevision(any(), anyList(), anyString(), anyString(), anyString());
  }
}
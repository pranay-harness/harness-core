package software.wings.delegatetasks.cloudformation;

import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
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
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationCreateStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationDeleteStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationListStacksHandler;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest.CloudFormationCommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationDeleteStackRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationListStacksRequest;

public class CloudFormationCommandTaskTest extends WingsBaseTest {
  @Mock private CloudFormationCreateStackHandler mockCreateStackHandler;
  @Mock private CloudFormationDeleteStackHandler mockDeleteStackHandler;
  @Mock private CloudFormationListStacksHandler mockListStacksHandler;

  @InjectMocks
  private CloudFormationCommandTask task =
      (CloudFormationCommandTask) TaskType.CLOUD_FORMATION_TASK.getDelegateRunnableTask("delegateid",
          DelegateTask.builder()
              .data(TaskData.builder().async(true).timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build())
              .build(),
          notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {
    on(task).set("createStackHandler", mockCreateStackHandler);
    on(task).set("deleteStackHandler", mockDeleteStackHandler);
    on(task).set("listStacksHandler", mockListStacksHandler);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testRun() {
    CloudFormationCreateStackRequest createStackRequest =
        CloudFormationCreateStackRequest.builder().commandType(CloudFormationCommandType.CREATE_STACK).build();
    task.run(new Object[] {createStackRequest, emptyList()});
    verify(mockCreateStackHandler).execute(any(), any());
    CloudFormationDeleteStackRequest deleteStackRequest =
        CloudFormationDeleteStackRequest.builder().commandType(CloudFormationCommandType.DELETE_STACK).build();
    task.run(new Object[] {deleteStackRequest, emptyList()});
    verify(mockDeleteStackHandler).execute(any(), any());
    CloudFormationListStacksRequest listStacksRequest =
        CloudFormationListStacksRequest.builder().commandType(CloudFormationCommandType.GET_STACKS).build();
    task.run(new Object[] {listStacksRequest, emptyList()});
    verify(mockListStacksHandler).execute(any(), any());
  }
}
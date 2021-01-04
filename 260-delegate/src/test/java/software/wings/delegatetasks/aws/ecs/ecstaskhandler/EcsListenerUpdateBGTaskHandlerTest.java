package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.ecs.request.EcsBGListenerUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsServiceDeployRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(Module._930_DELEGATE_TASKS)
public class EcsListenerUpdateBGTaskHandlerTest extends WingsBaseTest {
  @Mock private AwsElbHelperServiceDelegate awsElbHelperServiceDelegate;
  @Mock private EcsSwapRoutesCommandTaskHelper ecsSwapRoutesCommandTaskHelper;
  @Mock private ExecutionLogCallback executionLogCallback;

  @InjectMocks private EcsListenerUpdateBGTaskHandler ecsListenerUpdateBGTaskHandler;

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExecuteTaskInternalWithNotValidEcsCommandRequest() {
    EcsServiceDeployRequest ecsServiceDeployRequest = EcsServiceDeployRequest.builder().build();

    EcsCommandExecutionResponse ecsCommandExecutionResponse = ecsListenerUpdateBGTaskHandler.executeTaskInternal(
        ecsServiceDeployRequest, Collections.emptyList(), executionLogCallback);

    assertThat(ecsCommandExecutionResponse).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
    assertThat(ecsCommandExecutionResponse.getEcsCommandResponse().getOutput())
        .isEqualTo("Invalid Request Type: Expected was : EcsBGListenerUpdateRequest");
    assertThat(ecsCommandExecutionResponse.getEcsCommandResponse().getCommandExecutionStatus())
        .isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testRollbackWithUpdateRequired() {
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)
        .upsizeOlderService(any(), anyList(), anyString(), anyString(), anyInt(), anyString(), any(), anyInt());
    doNothing()
        .when(awsElbHelperServiceDelegate)
        .swapListenersForEcsBG(any(), anyList(), anyBoolean(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), any());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)
        .updateServiceTags(any(), anyList(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), any());
    doReturn(Arrays.asList(new Action()))
        .when(awsElbHelperServiceDelegate)
        .getMatchingTargetGroupForSpecificListenerRuleArn(
            any(), anyList(), anyString(), anyString(), anyString(), anyString(), any());

    EcsBGListenerUpdateRequest ecsBGListenerUpdateRequest =
        EcsBGListenerUpdateRequest.builder().rollback(true).isUseSpecificListenerRuleArn(true).build();
    EcsCommandExecutionResponse ecsCommandExecutionResponse = ecsListenerUpdateBGTaskHandler.executeTaskInternal(
        ecsBGListenerUpdateRequest, Collections.emptyList(), executionLogCallback);

    assertThat(ecsCommandExecutionResponse).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testRollbackWithNoUpdateRequired() {
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)
        .upsizeOlderService(any(), anyList(), anyString(), anyString(), anyInt(), anyString(), any(), anyInt());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)
        .updateServiceTags(any(), anyList(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), any());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)

        .downsizeOlderService(any(), anyList(), anyString(), anyString(), anyString(), any(), any());

    doReturn(Collections.emptyList())
        .when(awsElbHelperServiceDelegate)
        .getMatchingTargetGroupForSpecificListenerRuleArn(
            any(), anyList(), anyString(), anyString(), anyString(), anyString(), any());

    EcsBGListenerUpdateRequest ecsBGListenerUpdateRequest =
        EcsBGListenerUpdateRequest.builder().rollback(true).isUseSpecificListenerRuleArn(true).build();
    EcsCommandExecutionResponse ecsCommandExecutionResponse = ecsListenerUpdateBGTaskHandler.executeTaskInternal(
        ecsBGListenerUpdateRequest, Collections.emptyList(), executionLogCallback);

    assertThat(ecsCommandExecutionResponse).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownsizeOldService() {
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    doNothing()
        .when(awsElbHelperServiceDelegate)
        .swapListenersForEcsBG(any(), anyList(), anyBoolean(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), any());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)
        .updateServiceTags(any(), anyList(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), any());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)

        .downsizeOlderService(any(), anyList(), anyString(), anyString(), anyString(), any(), any());

    EcsBGListenerUpdateRequest ecsBGListenerUpdateRequest = EcsBGListenerUpdateRequest.builder()
                                                                .rollback(false)
                                                                .isUseSpecificListenerRuleArn(true)
                                                                .downsizeOldService(true)
                                                                .build();
    EcsCommandExecutionResponse ecsCommandExecutionResponse = ecsListenerUpdateBGTaskHandler.executeTaskInternal(
        ecsBGListenerUpdateRequest, Collections.emptyList(), executionLogCallback);

    assertThat(ecsCommandExecutionResponse).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testRollbackWithoutSpecificListenerRuleArn() {
    DescribeListenersResult describeListenersResult = new DescribeListenersResult();
    Action action = new Action();
    action.setType("forward");
    action.setTargetGroupArn("targetGroupArn");
    Listener listener = new Listener();
    listener.setDefaultActions(Arrays.asList(action));
    describeListenersResult.setListeners(Arrays.asList(listener));
    doNothing().when(executionLogCallback).saveExecutionLog(anyString());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)
        .upsizeOlderService(any(), anyList(), anyString(), anyString(), anyInt(), anyString(), any(), anyInt());
    doReturn(describeListenersResult)
        .when(awsElbHelperServiceDelegate)
        .describeListenerResult(any(), anyList(), anyString(), anyString());
    doNothing()
        .when(awsElbHelperServiceDelegate)
        .swapListenersForEcsBG(any(), anyList(), anyBoolean(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), any());
    doNothing()
        .when(ecsSwapRoutesCommandTaskHelper)
        .updateServiceTags(any(), anyList(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), any());

    EcsBGListenerUpdateRequest ecsBGListenerUpdateRequest =
        EcsBGListenerUpdateRequest.builder().rollback(true).isUseSpecificListenerRuleArn(false).build();
    EcsCommandExecutionResponse ecsCommandExecutionResponse = ecsListenerUpdateBGTaskHandler.executeTaskInternal(
        ecsBGListenerUpdateRequest, Collections.emptyList(), executionLogCallback);

    assertThat(ecsCommandExecutionResponse).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isNotNull();
    assertThat(ecsCommandExecutionResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }
}

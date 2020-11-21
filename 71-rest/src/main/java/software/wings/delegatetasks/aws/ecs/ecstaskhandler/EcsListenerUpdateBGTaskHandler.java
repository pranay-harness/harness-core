package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.data.structure.EmptyPredicate;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.helpers.ext.ecs.request.EcsBGListenerUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsListenerUpdateCommandResponse;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class EcsListenerUpdateBGTaskHandler extends EcsCommandTaskHandler {
  @Inject private AwsElbHelperServiceDelegate awsElbHelperServiceDelegate;
  @Inject private AwsHelperService awsHelperService;
  @Inject private EcsContainerService ecsContainerService;
  @Inject private EcsSwapRoutesCommandTaskHelper ecsSwapRoutesCommandTaskHelper;

  @Override
  public EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    EcsListenerUpdateCommandResponse ecsCommandResponse = EcsListenerUpdateCommandResponse.builder().build();
    if (!(ecsCommandRequest instanceof EcsBGListenerUpdateRequest)) {
      ecsCommandResponse.setOutput("Invalid Request Type: Expected was : EcsBGListenerUpdateRequest");
      ecsCommandResponse.setCommandExecutionStatus(CommandExecutionStatus.FAILURE);
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .ecsCommandResponse(ecsCommandResponse)
          .build();
    }

    EcsBGListenerUpdateRequest request = (EcsBGListenerUpdateRequest) ecsCommandRequest;
    if (request.isRollback()) {
      ecsSwapRoutesCommandTaskHelper.upsizeOlderService(request.getAwsConfig(), encryptedDataDetails,
          request.getRegion(), request.getCluster(), request.getServiceCountDownsized(),
          request.getServiceNameDownsized(), executionLogCallback, request.getServiceSteadyStateTimeout());
    }

    if (isUpdateRequired(request, encryptedDataDetails, executionLogCallback)) {
      logListenerUpdateDetails(request, executionLogCallback);
      awsElbHelperServiceDelegate.swapListenersForEcsBG(request.getAwsConfig(), encryptedDataDetails,
          request.isUseSpecificListenerRuleArn(), request.getProdListenerArn(), request.getStageListenerArn(),
          request.getProdListenerRuleArn(), request.getStageListenerRuleArn(), request.getTargetGroupForNewService(),
          request.getTargetGroupForExistingService(), request.getRegion(), executionLogCallback);
      executionLogCallback.saveExecutionLog("Successfully update Prod and Stage Listeners");
    } else {
      executionLogCallback.saveExecutionLog(format("Not swapping target groups, prod: [%s], stage: [%s]",
          request.getTargetGroupForExistingService(), request.getTargetGroupForNewService()));
    }

    ecsSwapRoutesCommandTaskHelper.updateServiceTags(request.getAwsConfig(), encryptedDataDetails, request.getRegion(),
        request.getCluster(), request.getServiceName(), request.getServiceNameDownsized(), request.isRollback(),
        executionLogCallback);

    if (!request.isRollback() && request.isDownsizeOldService()) {
      ecsSwapRoutesCommandTaskHelper.downsizeOlderService(request.getAwsConfig(), encryptedDataDetails,
          request.getRegion(), request.getCluster(), request.getServiceNameDownsized(), executionLogCallback,
          request.getServiceSteadyStateTimeout());
    }

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    ecsCommandResponse.setCommandExecutionStatus(commandExecutionStatus);
    return EcsCommandExecutionResponse.builder()
        .commandExecutionStatus(commandExecutionStatus)
        .ecsCommandResponse(ecsCommandResponse)
        .build();
  }

  private void logListenerUpdateDetails(EcsBGListenerUpdateRequest request, ExecutionLogCallback executionLogCallback) {
    if (!request.isRollback()) {
      executionLogCallback.saveExecutionLog(
          new StringBuilder(128)
              .append(
                  "Updating ELB Prod Listener to Forward requests to Target group associated with new Service, TargetGroup: ")
              .append(request.getTargetGroupForNewService())
              .toString());

      if (isNotEmpty(request.getStageListenerArn())) {
        executionLogCallback.saveExecutionLog(
            new StringBuilder(128)
                .append(
                    "Updating ELB Stage Listener to Forward requests to Target group associated with new Service, TargetGroup: ")
                .append(request.getTargetGroupForExistingService())
                .toString());
      }
    } else {
      executionLogCallback.saveExecutionLog(
          new StringBuilder(128)
              .append(
                  "Updating ELB Prod Listener to Forward requests to Target group associated with Old Service, TargetGroup: ")
              .append(request.getTargetGroupForExistingService())
              .toString());

      if (isNotEmpty(request.getStageListenerArn())) {
        executionLogCallback.saveExecutionLog(
            new StringBuilder(128)
                .append(
                    "Updating ELB Stage Listener to Forward requests to Target group associated with new Service, TargetGroup: ")
                .append(request.getTargetGroupForNewService())
                .toString());
      }
    }
  }

  private boolean isUpdateRequired(EcsBGListenerUpdateRequest request, List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback) {
    if (!request.isRollback()) {
      return true;
    }

    if (request.isUseSpecificListenerRuleArn()) {
      List<Action> actions = awsElbHelperServiceDelegate.getMatchingTargetGroupForSpecificListenerRuleArn(
          request.getAwsConfig(), encryptedDataDetails, request.getRegion(), request.getProdListenerArn(),
          request.getProdListenerRuleArn(), request.getTargetGroupForNewService(), executionLogCallback);
      if (EmptyPredicate.isNotEmpty(actions)) {
        return true;
      }
      return false;
    }

    DescribeListenersResult result = awsElbHelperServiceDelegate.describeListenerResult(
        request.getAwsConfig(), encryptedDataDetails, request.getProdListenerArn(), request.getRegion());

    List<Action> actions = result.getListeners().get(0).getDefaultActions();
    Optional<Action> optionalAction =
        actions.stream()
            .filter(action -> "forward".equalsIgnoreCase(action.getType()) && isNotEmpty(action.getTargetGroupArn()))
            .findFirst();

    String arn = optionalAction.get().getTargetGroupArn();

    if (arn.equalsIgnoreCase(request.getTargetGroupForNewService())) {
      return true;
    }
    return false;
  }
}

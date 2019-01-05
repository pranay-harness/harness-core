package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.common.Constants.BG_BLUE;
import static software.wings.common.Constants.BG_GREEN;
import static software.wings.common.Constants.BG_VERSION;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.TagResourceRequest;
import com.amazonaws.services.ecs.model.UntagResourceRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.helpers.ext.ecs.request.EcsBGListenerUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.helpers.ext.ecs.response.EcsListenerUpdateCommandResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.List;
import java.util.Optional;

public class EcsListenerUpdateBGTaskHandler extends EcsCommandTaskHandler {
  private static final Logger logger = LoggerFactory.getLogger(EcsListenerUpdateBGTaskHandler.class);
  private static final String DELIMITER = "__";
  @Inject private AwsEcsHelperServiceDelegate awsEcsHelperServiceDelegate;
  @Inject private AwsHelperService awsHelperService;
  @Inject private EcsContainerService ecsContainerService;

  public EcsCommandExecutionResponse executeTaskInternal(
      EcsCommandRequest ecsCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
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
      upsizeOlderService(request, encryptedDataDetails, executionLogCallback);
    }

    if (isUpdateRequired(request, encryptedDataDetails)) {
      logListenerUpdateDetails(request);

      awsEcsHelperServiceDelegate.updateListenersForEcsBG(request.getAwsConfig(), encryptedDataDetails,
          request.getProdListenerArn(), request.getStageListenerArn(), request.getRegion());

      executionLogCallback.saveExecutionLog("Successfully update Prod and Stage Listeners");
    }

    updateServiceTags(request, encryptedDataDetails);

    if (!request.isRollback() && request.isDownsizeOldService()) {
      downsizeOlderService(request, encryptedDataDetails);
    }

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.SUCCESS;
    ecsCommandResponse.setCommandExecutionStatus(commandExecutionStatus);
    return EcsCommandExecutionResponse.builder()
        .commandExecutionStatus(commandExecutionStatus)
        .ecsCommandResponse(ecsCommandResponse)
        .build();
  }

  private void logListenerUpdateDetails(EcsBGListenerUpdateRequest request) {
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

  private void updateServiceTags(EcsBGListenerUpdateRequest request, List<EncryptedDataDetail> encryptedDataDetails) {
    DescribeServicesResult newService =
        awsHelperService.describeServices(request.getRegion(), request.getAwsConfig(), encryptedDataDetails,
            new DescribeServicesRequest().withCluster(request.getCluster()).withServices(request.getServiceName()));

    DescribeServicesResult oldService = null;
    if (isNotBlank(request.getServiceNameDownsized())) {
      oldService = awsHelperService.describeServices(request.getRegion(), request.getAwsConfig(), encryptedDataDetails,
          new DescribeServicesRequest()
              .withCluster(request.getCluster())
              .withServices(request.getServiceNameDownsized()));
    }

    Service blueService;
    Service greenService;

    if (request.isRollback()) {
      // This was first deploy, so there was no prev "BLUE" service
      blueService = oldService == null ? null : oldService.getServices().get(0);
      greenService = newService.getServices().get(0);
    } else {
      // This is first deploy, so there is no "GREEN" service
      greenService = oldService == null ? null : oldService.getServices().get(0);
      blueService = newService.getServices().get(0);
    }

    if (blueService != null) {
      executionLogCallback.saveExecutionLog(new StringBuilder(128)
                                                .append("Updating Service: ")
                                                .append(blueService.getServiceName())
                                                .append(" with Tag: <")
                                                .append(BG_VERSION)
                                                .append(":")
                                                .append(BG_BLUE)
                                                .append(">")
                                                .toString());

      awsHelperService.untagService(request.getRegion(), encryptedDataDetails,
          new UntagResourceRequest().withResourceArn(blueService.getServiceArn()).withTagKeys(BG_VERSION),
          request.getAwsConfig());

      awsHelperService.tagService(request.getRegion(), encryptedDataDetails,
          new TagResourceRequest()
              .withResourceArn(blueService.getServiceArn())
              .withTags(new com.amazonaws.services.ecs.model.Tag().withKey(BG_VERSION).withValue(BG_BLUE)),
          request.getAwsConfig());

      executionLogCallback.saveExecutionLog("Tag update successful");
    }

    if (greenService != null) {
      executionLogCallback.saveExecutionLog(new StringBuilder(128)
                                                .append("Updating Service: ")
                                                .append(greenService.getServiceName())
                                                .append(" with Tag: <")
                                                .append(BG_VERSION)
                                                .append(":")
                                                .append(BG_GREEN)
                                                .append(">")
                                                .toString());

      awsHelperService.untagService(request.getRegion(), encryptedDataDetails,
          new UntagResourceRequest().withResourceArn(greenService.getServiceArn()).withTagKeys(BG_VERSION),
          request.getAwsConfig());

      awsHelperService.tagService(request.getRegion(), encryptedDataDetails,
          new TagResourceRequest()
              .withResourceArn(greenService.getServiceArn())
              .withTags(new com.amazonaws.services.ecs.model.Tag().withKey(BG_VERSION).withValue(BG_GREEN)),
          request.getAwsConfig());
      executionLogCallback.saveExecutionLog("Tag update successful");
    }
  }

  private void upsizeOlderService(EcsBGListenerUpdateRequest request, List<EncryptedDataDetail> encryptedDataDetails,
      ExecutionLogCallback executionLogCallback) {
    if (isBlank(request.getServiceNameDownsized())) {
      executionLogCallback.saveExecutionLog("No service needs to be upsized");
      return;
    }

    ecsContainerService.updateServiceCount(request.getRegion(), encryptedDataDetails, request.getCluster(),
        request.getServiceNameDownsized(), request.getServiceCountDownsized(), this.executionLogCallback,
        request.getAwsConfig());

    this.executionLogCallback.saveExecutionLog(
        new StringBuilder("Service update request successfully submitted for Service: ")
            .append(request.getServiceCountDownsized())
            .append(", with Count: ")
            .append(request.getServiceCountDownsized())
            .toString(),
        LogLevel.INFO);

    ecsContainerService.waitForTasksToBeInRunningStateButDontThrowException(request.getRegion(), request.getAwsConfig(),
        encryptedDataDetails, request.getCluster(), request.getServiceNameDownsized(), this.executionLogCallback,
        request.getServiceCountDownsized());

    ecsContainerService.waitForServiceToReachSteadyState(request.getRegion(), request.getAwsConfig(),
        encryptedDataDetails, request.getCluster(), request.getServiceNameDownsized(), 20, this.executionLogCallback);
  }

  private void downsizeOlderService(
      EcsBGListenerUpdateRequest request, List<EncryptedDataDetail> encryptedDataDetails) {
    if (isNotBlank(request.getServiceNameDownsized())) {
      executionLogCallback.saveExecutionLog("Downsizing Green Service: " + request.getServiceNameDownsized());

      ecsContainerService.updateServiceCount(request.getRegion(), encryptedDataDetails, request.getCluster(),
          request.getServiceNameDownsized(), 0, this.executionLogCallback, request.getAwsConfig());
    } else {
      executionLogCallback.saveExecutionLog("No Service needs to be downsized");
    }
  }

  private boolean isUpdateRequired(EcsBGListenerUpdateRequest request, List<EncryptedDataDetail> encryptedDataDetails) {
    if (!request.isRollback()) {
      return true;
    }

    DescribeListenersResult result = awsEcsHelperServiceDelegate.describeListenerResult(
        request.getAwsConfig(), encryptedDataDetails, request.getProdListenerArn(), request.getRegion());

    List<Action> actions = result.getListeners().get(0).getDefaultActions();
    Optional<Action> optionalAction =
        actions.stream()
            .filter(action -> "forward".equalsIgnoreCase(action.getType()) && isNotEmpty(action.getTargetGroupArn()))
            .findFirst();

    String arn = optionalAction.get().getTargetGroupArn();
    if (arn.equalsIgnoreCase(request.getTargetGroupForNewService())) {
      return true;
    } else {
      return false;
    }
  }
}

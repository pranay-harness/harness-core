package software.wings.delegatetasks.spotinst;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.spotinst.taskhandler.SpotInstDeployTaskHandler;
import software.wings.delegatetasks.spotinst.taskhandler.SpotInstSetupTaskHandler;
import software.wings.delegatetasks.spotinst.taskhandler.SpotInstSwapRoutesTaskHandler;
import software.wings.delegatetasks.spotinst.taskhandler.SpotInstSyncTaskHandler;
import software.wings.delegatetasks.spotinst.taskhandler.SpotInstTaskHandler;
import software.wings.delegatetasks.spotinst.taskhandler.SpotinstTrafficShiftAlbDeployTaskHandler;
import software.wings.delegatetasks.spotinst.taskhandler.SpotinstTrafficShiftAlbSetupTaskHandler;
import software.wings.delegatetasks.spotinst.taskhandler.SpotinstTrafficShiftAlbSwapRoutesTaskHandler;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.intfc.security.EncryptionService;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class SpotInstTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private SpotInstSyncTaskHandler syncTaskHandler;
  @Inject private SpotInstSetupTaskHandler setupTaskHandler;
  @Inject private SpotInstDeployTaskHandler deployTaskHandler;
  @Inject private SpotInstSwapRoutesTaskHandler swapRoutesTaskHandler;
  @Inject private SpotinstTrafficShiftAlbSetupTaskHandler shiftAlbSetupTaskHandler;
  @Inject private SpotinstTrafficShiftAlbDeployTaskHandler shiftAlbDeployTaskHandler;
  @Inject private SpotinstTrafficShiftAlbSwapRoutesTaskHandler shiftAlbSwapRoutesTaskHandler;

  public SpotInstTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public SpotInstTaskExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented.");
  }

  @Override
  public SpotInstTaskExecutionResponse run(TaskParameters parameters) {
    if (!(parameters instanceof SpotInstCommandRequest)) {
      String message =
          format("Unrecognized task params while running spot inst task: [%s]", parameters.getClass().getSimpleName());
      logger.error(message);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    SpotInstCommandRequest spotInstCommandRequest = (SpotInstCommandRequest) parameters;
    SpotInstTaskParameters spotInstTaskParameters = spotInstCommandRequest.getSpotInstTaskParameters();

    if (spotInstCommandRequest.getAwsConfig() != null) {
      encryptionService.decrypt(
          spotInstCommandRequest.getAwsConfig(), spotInstCommandRequest.getAwsEncryptionDetails());
    }

    encryptionService.decrypt(
        spotInstCommandRequest.getSpotInstConfig(), spotInstCommandRequest.getSpotinstEncryptionDetails());

    SpotInstTaskHandler handler;
    if (spotInstTaskParameters.isSyncTask()) {
      handler = syncTaskHandler;
    } else {
      switch (spotInstTaskParameters.getCommandType()) {
        case SPOT_INST_SETUP: {
          handler = setupTaskHandler;
          break;
        }
        case SPOT_INST_DEPLOY: {
          handler = deployTaskHandler;
          break;
        }
        case SPOT_INST_SWAP_ROUTES: {
          handler = swapRoutesTaskHandler;
          break;
        }
        case SPOT_INST_ALB_SHIFT_SETUP:
          handler = shiftAlbSetupTaskHandler;
          break;
        case SPOT_INST_ALB_SHIFT_DEPLOY:
          handler = shiftAlbDeployTaskHandler;
          break;
        case SPOT_INST_ALB_SHIFT_SWAP_ROUTES:
          handler = shiftAlbSwapRoutesTaskHandler;
          break;
        default: {
          String message =
              format("Unrecognized task params type running spot inst task: [%s]. Workflow execution: [%s]",
                  spotInstTaskParameters.getCommandType().name(), spotInstTaskParameters.getWorkflowExecutionId());
          logger.error(message);
          return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
        }
      }
    }
    return handler.executeTask(
        spotInstTaskParameters, spotInstCommandRequest.getSpotInstConfig(), spotInstCommandRequest.getAwsConfig());
  }
}
package software.wings.delegatetasks.cloudformation;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationCommandTaskHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationCreateStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationDeleteStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationListStacksHandler;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class CloudFormationCommandTask extends AbstractDelegateRunnableTask {
  @Inject private CloudFormationCreateStackHandler createStackHandler;
  @Inject private CloudFormationDeleteStackHandler deleteStackHandler;
  @Inject private CloudFormationListStacksHandler listStacksHandler;

  public CloudFormationCommandTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public CloudFormationCommandExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public CloudFormationCommandExecutionResponse run(Object[] parameters) {
    CloudFormationCommandRequest request = (CloudFormationCommandRequest) parameters[0];
    List<EncryptedDataDetail> details = (List<EncryptedDataDetail>) parameters[1];

    CloudFormationCommandTaskHandler handler = null;
    switch (request.getCommandType()) {
      case GET_STACKS: {
        handler = listStacksHandler;
        break;
      }
      case CREATE_STACK: {
        handler = createStackHandler;
        break;
      }
      case DELETE_STACK: {
        handler = deleteStackHandler;
        break;
      }
      default: {
        return CloudFormationCommandExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(String.format("Unidentified command task type: %s", request.getCommandType().name()))
            .build();
      }
    }
    try {
      return handler.execute(request, details);
    } catch (Exception ex) {
      logger.error("Exception in processing cloud formation task [{}]", request.toString(), ex);
      return CloudFormationCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }
}

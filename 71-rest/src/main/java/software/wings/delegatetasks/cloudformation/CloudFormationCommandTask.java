package software.wings.delegatetasks.cloudformation;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.protocol.TaskParameters;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationCommandTaskHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationCreateStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationDeleteStackHandler;
import software.wings.delegatetasks.cloudformation.cloudformationtaskhandler.CloudFormationListStacksHandler;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.utils.Misc;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CloudFormationCommandTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(CloudFormationCommandTask.class);
  @Inject private CloudFormationCreateStackHandler createStackHandler;
  @Inject private CloudFormationDeleteStackHandler deleteStackHandler;
  @Inject private CloudFormationListStacksHandler listStacksHandler;

  public CloudFormationCommandTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
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
      logger.error(format("Exception in processing cloud formation task [%s]", request.toString()), ex);
      return CloudFormationCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(Misc.getMessage(ex))
          .build();
    }
  }
}

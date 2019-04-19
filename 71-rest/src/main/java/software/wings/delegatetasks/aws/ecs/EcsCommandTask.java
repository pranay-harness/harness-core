package software.wings.delegatetasks.aws.ecs;

import static java.lang.String.format;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskResponse;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsCommandTaskHandler;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
@Slf4j
public class EcsCommandTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, EcsCommandTaskHandler> commandTaskTypeToTaskHandlerMap;

  public EcsCommandTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public EcsCommandExecutionResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public EcsCommandExecutionResponse run(Object[] parameters) {
    EcsCommandRequest ecsCommandRequest = (EcsCommandRequest) parameters[0];

    try {
      return commandTaskTypeToTaskHandlerMap.get(ecsCommandRequest.getEcsCommandType().name())
          .executeTask(ecsCommandRequest, (List) parameters[1]);
    } catch (WingsException ex) {
      logger.error(format("Exception in processing ECS task [%s]", ecsCommandRequest.toString()), ex);
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }
}

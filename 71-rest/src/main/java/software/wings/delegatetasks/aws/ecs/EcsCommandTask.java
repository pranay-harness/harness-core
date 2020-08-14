package software.wings.delegatetasks.aws.ecs;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.delegatetasks.aws.ecs.ecstaskhandler.EcsCommandTaskHandler;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;

import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
@Slf4j
public class EcsCommandTask extends AbstractDelegateRunnableTask {
  @Inject private Map<String, EcsCommandTaskHandler> commandTaskTypeToTaskHandlerMap;

  public EcsCommandTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
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
      logger.error("Exception in processing ECS task [{}]", ecsCommandRequest.toString(), ex);
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .build();
    }
  }
}

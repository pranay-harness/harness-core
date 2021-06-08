package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.terraform.handlers.TerraformAbstractTaskHandler;
import io.harness.exception.UnexpectedTypeException;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@OwnedBy(CDP)
public class AwsSamTaskHandlerNG extends AbstractDelegateRunnableTask {
  @Inject private Map<AwsSamTaskType, AwsSamAbstractTaskHandler> awsSamAbstractTaskHandlerMap;

  public AwsSamTaskHandlerNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    AwsSamTaskParameters taskParameters = (AwsSamTaskParameters) parameters;
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    LogCallback logCallback = getLogCallback(
        getLogStreamingTaskClient(), taskParameters.getAwsSamCommandUnit().name(), true, commandUnitsProgress);

    if (!awsSamAbstractTaskHandlerMap.containsKey(taskParameters.getAwsSamTaskType())) {
      throw new UnexpectedTypeException(
          String.format("Unexpected Terraform Task Type: [%s]", taskParameters.getAwsSamTaskType()));
    }

    AwsSamAbstractTaskHandler taskHandler = awsSamAbstractTaskHandlerMap.get(taskParameters.getAwsSamTaskType());
    return taskHandler.executeTask(taskParameters, getDelegateId(), getTaskId(), logCallback, commandUnitsProgress);
  }

  public LogCallback getLogCallback(ILogStreamingTaskClient logStreamingTaskClient, String commandUnitName,
      boolean shouldOpenStream, CommandUnitsProgress commandUnitsProgress) {
    return new NGDelegateLogCallback(logStreamingTaskClient, commandUnitName, shouldOpenStream, commandUnitsProgress);
  }
}

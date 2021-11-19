package io.harness.delegate.task.citasks;

import static java.lang.String.format;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ngexception.CIStageExecutionException;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.commons.lang3.NotImplementedException;

public class CIExecuteStepTask extends AbstractDelegateRunnableTask {
  @Inject @Named(CITaskConstants.EXECUTE_STEP_AWS_VM) private CIExecuteStepTaskHandler ciAwsVmExecuteStepTaskHandler;
  @Inject @Named(CITaskConstants.EXECUTE_STEP_K8) private CIExecuteStepTaskHandler ciK8ExecuteStepTaskHandler;

  public CIExecuteStepTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CIExecuteStepTaskParams ciExecuteStepTaskParams = (CIExecuteStepTaskParams) parameters;
    if (ciExecuteStepTaskParams.getType() == CIExecuteStepTaskParams.Type.K8) {
      return ciK8ExecuteStepTaskHandler.executeTaskInternal(ciExecuteStepTaskParams);
    } else if (ciExecuteStepTaskParams.getType() == CIExecuteStepTaskParams.Type.AWS_VM) {
      return ciAwsVmExecuteStepTaskHandler.executeTaskInternal(ciExecuteStepTaskParams);
    } else {
      throw new CIStageExecutionException(
          format("Invalid infra type for executing step", ciExecuteStepTaskParams.getType()));
    }
  }
}

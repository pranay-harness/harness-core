package io.harness.delegate.task.citasks;

/**
 * Delegate task to setup CI build environment. It calls CIK8BuildTaskHandler class to setup the build environment on
 * K8.
 */

import static java.lang.String.format;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ci.CIInitializeTaskParams;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ngexception.CIStageExecutionException;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class CIInitializeTask extends AbstractDelegateRunnableTask {
  @Inject @Named(CITaskConstants.INIT_AWS_VM) private CIInitializeTaskHandler ciAwsVmInitializeTaskHandler;
  @Inject @Named(CITaskConstants.INIT_K8) private CIInitializeTaskHandler ciK8InitializeTaskHandler;

  public CIInitializeTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CIInitializeTaskParams ciInitializeTaskParams = (CIInitializeTaskParams) parameters;
    if (ciInitializeTaskParams.getType() == CIInitializeTaskParams.Type.GCP_K8) {
      return ciK8InitializeTaskHandler.executeTaskInternal(ciInitializeTaskParams, getLogStreamingTaskClient());
    } else if (ciInitializeTaskParams.getType() == CIInitializeTaskParams.Type.AWS_VM) {
      return ciAwsVmInitializeTaskHandler.executeTaskInternal(ciInitializeTaskParams, getLogStreamingTaskClient());
    } else {
      throw new CIStageExecutionException(
          format("Invalid infra type for initializing stage", ciInitializeTaskParams.getType()));
    }
  }
}

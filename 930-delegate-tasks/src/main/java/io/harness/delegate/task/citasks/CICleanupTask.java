package io.harness.delegate.task.citasks;

/**
 * Delegate task to setup CI setup build environment.
 */

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ci.CICleanupTaskParams;
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
public class CICleanupTask extends AbstractDelegateRunnableTask {
  @Inject @Named(CITaskConstants.CLEANUP_AWS_VM) private CICleanupTaskHandler ciAwsVmCleanupTaskHandler;
  @Inject @Named(CITaskConstants.CLEANUP_K8) private CICleanupTaskHandler ciK8CleanupTaskHandler;

  public CICleanupTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CICleanupTaskParams ciCleanupTaskParams = (CICleanupTaskParams) parameters;
    if (ciCleanupTaskParams.getType() == CICleanupTaskParams.Type.GCP_K8) {
      return ciK8CleanupTaskHandler.executeTaskInternal(ciCleanupTaskParams);
    } else if (ciCleanupTaskParams.getType() == CICleanupTaskParams.Type.AWS_VM) {
      return ciAwsVmCleanupTaskHandler.executeTaskInternal(ciCleanupTaskParams);
    } else {
      throw new CIStageExecutionException(
          String.format("Invalid infra type for cleanup step", ciCleanupTaskParams.getType()));
    }
  }
}

package software.wings.delegatetasks.citasks;

/**
 * Delegate task to setup CI setup build environment.
 */

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.ci.CICleanupTaskParams;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class CICleanupTask extends AbstractDelegateRunnableTask {
  @Inject private CICleanupTaskHandler ciCleanupTaskHandler;

  public CICleanupTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    CICleanupTaskParams ciCleanupTaskParams = (CICleanupTaskParams) parameters;
    return ciCleanupTaskHandler.executeTaskInternal(ciCleanupTaskParams);
  }
}

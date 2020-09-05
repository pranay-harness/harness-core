package software.wings.delegatetasks.citasks;

/**
 * Delegate task to setup CI build environment. It calls CIK8BuildTaskHandler class to setup the build environment on
 * K8.
 */

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.ci.CIBuildSetupTaskParams;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class CIBuildCommandTask extends AbstractDelegateRunnableTask {
  @Inject private CIBuildTaskHandler ciBuildTaskHandler;

  public CIBuildCommandTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    CIBuildSetupTaskParams ciBuildSetupTaskParams = (CIBuildSetupTaskParams) parameters;
    return ciBuildTaskHandler.executeTaskInternal(ciBuildSetupTaskParams);
  }
}
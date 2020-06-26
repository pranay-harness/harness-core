package software.wings.delegatetasks.container;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/*
 This is a dummy task added to support container instance sync via perpetual task. Container Instance sync runs using
 delegate Proxy Factory and sync tasks which cannot be supported as validation tasks in the perpetual task framework.
 All we want is the validation to run and the task being assigned to some valid delegate.
 */
public class ContainerDummyTask extends AbstractDelegateRunnableTask {
  public ContainerDummyTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    return null;
  }
}

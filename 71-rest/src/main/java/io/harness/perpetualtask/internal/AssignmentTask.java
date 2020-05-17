package io.harness.perpetualtask.internal;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class AssignmentTask extends AbstractDelegateRunnableTask {
  public AssignmentTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AssignmentTaskResponse run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public AssignmentTaskResponse run(TaskParameters parameters) {
    logger.debug("Delegate id: {}", getDelegateId());
    return AssignmentTaskResponse.builder().delegateId(getDelegateId()).build();
  }
}

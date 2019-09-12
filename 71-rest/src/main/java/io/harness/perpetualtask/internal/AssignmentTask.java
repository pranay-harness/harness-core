package io.harness.perpetualtask.internal;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class AssignmentTask extends AbstractDelegateRunnableTask {
  public AssignmentTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
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

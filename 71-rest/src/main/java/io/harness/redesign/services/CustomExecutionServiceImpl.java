package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

@OwnedBy(CDC)
@Redesign
public class CustomExecutionServiceImpl implements CustomExecutionService {
  @Inject private ExecutionEngine engine;
  @Inject private InterruptManager interruptManager;

  @Override
  public PlanExecution executeHttpSwitch() {
    return engine.startExecution(CustomExecutionUtils.provideHttpSwitchPlan(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeHttpFork() {
    return engine.startExecution(CustomExecutionUtils.provideHttpForkPlan(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    return engine.startExecution(CustomExecutionUtils.provideHttpSectionPlan(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryPlan() {
    return engine.startExecution(CustomExecutionUtils.provideHttpRetryPlan(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRollbackPlan() {
    User user = UserThreadLocal.get();
    return engine.startExecution(CustomExecutionUtils.provideHttpRollbackPlan(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  @Override
  public PlanExecution executeSimpleShellScriptPlan() {
    return engine.startExecution(CustomExecutionUtils.provideSimpleShellScriptPlan(), getEmbeddedUser());
  }

  @Override
  public Interrupt registerInterrupt(String planExecutionId) {
    return interruptManager.register(planExecutionId, ABORT_ALL, getEmbeddedUser(), null);
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }
}

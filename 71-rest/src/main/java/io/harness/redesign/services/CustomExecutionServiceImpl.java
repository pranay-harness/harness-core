package io.harness.redesign.services;

import com.google.inject.Inject;

import io.harness.annotations.Redesign;
import io.harness.beans.EmbeddedUser;
import io.harness.engine.ExecutionEngine;
import io.harness.state.execution.PlanExecution;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

@Redesign
public class CustomExecutionServiceImpl implements CustomExecutionService {
  @Inject private ExecutionEngine engine;

  @Override
  public PlanExecution executeHttpSwitch() {
    User user = UserThreadLocal.get();
    return engine.startExecution(CustomExecutionUtils.provideHttpSwitchPlan(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  @Override
  public PlanExecution executeHttpFork() {
    User user = UserThreadLocal.get();
    return engine.startExecution(CustomExecutionUtils.provideHttpForkPlan(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    User user = UserThreadLocal.get();
    return engine.startExecution(CustomExecutionUtils.provideHttpSectionPlan(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }
}

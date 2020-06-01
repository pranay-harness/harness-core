package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.engine.ExecutionEngine;
import io.harness.engine.GraphGenerator;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.execution.PlanExecution;
import io.harness.interrupts.Interrupt;
import io.harness.plan.input.InputArgs;
import io.harness.resource.Graph;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

@OwnedBy(CDC)
@Redesign
@Singleton
public class CustomExecutionServiceImpl implements CustomExecutionService {
  @Inject private ExecutionEngine engine;
  @Inject private InterruptManager interruptManager;
  @Inject private GraphGenerator graphGenerator;

  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String APP_ID = "d9cTupsyQjWqbhUmZ8XPdQ";

  @Override
  public PlanExecution executeHttpSwitch() {
    return engine.startExecution(CustomExecutionUtils.provideHttpSwitchPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeHttpFork() {
    return engine.startExecution(CustomExecutionUtils.provideHttpForkPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    return engine.startExecution(CustomExecutionUtils.provideHttpSectionPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryPlan() {
    return engine.startExecution(CustomExecutionUtils.provideHttpRetryPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRollbackPlan() {
    User user = UserThreadLocal.get();
    return engine.startExecution(CustomExecutionUtils.provideHttpRollbackPlan(), getInputArgs(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  @Override
  public PlanExecution executeSimpleShellScriptPlan() {
    return engine.startExecution(
        CustomExecutionUtils.provideSimpleShellScriptPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeTaskChainPlan() {
    return engine.startExecution(CustomExecutionUtils.provideTaskChainPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainPlan() {
    return engine.startExecution(CustomExecutionUtils.provideSectionChainPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testInfraState() {
    return engine.startExecution(CustomExecutionUtils.provideInfraStateTestPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testGraphPlan() {
    return engine.startExecution(CustomExecutionUtils.provideGraphTestPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public Interrupt registerInterrupt(String planExecutionId) {
    return interruptManager.register(planExecutionId, ABORT_ALL, getEmbeddedUser(), null);
  }

  @Override
  public Graph getGraph(String planExecutionId) {
    return Graph.builder().graphVertex(graphGenerator.generateGraphVertex(planExecutionId)).build();
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }

  private InputArgs getInputArgs() {
    return InputArgs.builder().put("accountId", ACCOUNT_ID).put("appId", APP_ID).build();
  }
}

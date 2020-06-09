package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.engine.EngineService;
import io.harness.engine.GraphGenerator;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.engine.services.PlanExecutionService;
import io.harness.exception.GeneralException;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;
import io.harness.plan.input.InputArgs;
import io.harness.presentation.Graph;
import io.harness.yaml.utils.YamlPipelineUtils;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import java.io.IOException;

@OwnedBy(CDC)
@Redesign
@Singleton
public class CustomExecutionServiceImpl implements CustomExecutionService {
  @Inject private EngineService engineService;
  @Inject private InterruptManager interruptManager;
  @Inject private GraphGenerator graphGenerator;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;
  @Inject private PlanExecutionService planExecutionService;

  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String APP_ID = "d9cTupsyQjWqbhUmZ8XPdQ";

  @Override
  public PlanExecution executeHttpSwitch() {
    return engineService.startExecution(
        CustomExecutionUtils.provideHttpSwitchPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeHttpFork() {
    return engineService.startExecution(CustomExecutionUtils.provideHttpForkPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    return engineService.startExecution(
        CustomExecutionUtils.provideHttpSectionPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryIgnorePlan() {
    return engineService.startExecution(
        CustomExecutionUtils.provideHttpRetryIgnorePlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryAbortPlan() {
    return engineService.startExecution(
        CustomExecutionUtils.provideHttpRetryAbortPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRollbackPlan() {
    User user = UserThreadLocal.get();
    return engineService.startExecution(CustomExecutionUtils.provideHttpRollbackPlan(), getInputArgs(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  @Override
  public PlanExecution executeSimpleShellScriptPlan(String accountId, String appId) {
    return engineService.startExecution(
        CustomExecutionUtils.provideSimpleShellScriptPlan(), getInputArgs(accountId, appId), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeTaskChainPlan() {
    return engineService.startExecution(CustomExecutionUtils.provideTaskChainPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainPlan() {
    return engineService.startExecution(
        CustomExecutionUtils.provideSectionChainPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testInfraState() {
    return engineService.startExecution(
        CustomExecutionUtils.provideInfraStateTestPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testGraphPlan() {
    return engineService.startExecution(CustomExecutionUtils.provideGraphTestPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testArtifactState() {
    return engineService.startExecution(
        CustomExecutionUtils.provideArtifactStateTestPlan(), getInputArgs(), getEmbeddedUser());
  }

  @Override
  public Interrupt registerInterrupt(String planExecutionId) {
    return interruptManager.register(InterruptPackage.builder()
                                         .planExecutionId(planExecutionId)
                                         .interruptType(ABORT_ALL)
                                         .embeddedUser(getEmbeddedUser())
                                         .build());
  }

  @Override
  public Graph getGraph(String planExecutionId) {
    PlanExecution planExecution = planExecutionService.get(planExecutionId);
    return Graph.builder()
        .planExecutionId(planExecution.getUuid())
        .startTs(planExecution.getStartTs())
        .endTs(planExecution.getEndTs())
        .status(planExecution.getStatus())
        .graphVertex(graphGenerator.generateGraphVertex(planExecution.getUuid()))
        .build();
  }

  @Override
  public PlanExecution testExecutionPlanCreator(String pipelineYaml, String accountId, String appId) {
    final CDPipeline cdPipeline;
    try {
      cdPipeline = YamlPipelineUtils.read(pipelineYaml, CDPipeline.class);
      final Plan planForPipeline = executionPlanCreatorService.createPlanForPipeline(cdPipeline, accountId);
      return engineService.startExecution(planForPipeline,
          InputArgs.builder().put("accountId", accountId).put("appId", appId).build(), getEmbeddedUser());
    } catch (IOException e) {
      throw new GeneralException("error while testing execution plan", e);
    }
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }

  private InputArgs getInputArgs() {
    return getInputArgs(ACCOUNT_ID, APP_ID);
  }

  private InputArgs getInputArgs(String accountId, String appId) {
    return InputArgs.builder().put("accountId", accountId).put("appId", appId).build();
  }
}

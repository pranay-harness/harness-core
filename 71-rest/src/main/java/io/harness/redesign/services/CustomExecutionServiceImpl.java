package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.Graph;
import io.harness.dto.OrchestrationGraph;
import io.harness.engine.OrchestrationService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.facilitator.FacilitatorType;
import io.harness.generator.GraphVisualizer;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;
import io.harness.service.GraphGenerationService;
import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
@Singleton
public class CustomExecutionServiceImpl implements CustomExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private InterruptManager interruptManager;
  @Inject private ExecutionPlanCreatorService executionPlanCreatorService;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private GraphVisualizer graphVisualizer;
  @Inject private CustomExecutionProvider customExecutionProvider;

  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String APP_ID = "d9cTupsyQjWqbhUmZ8XPdQ";

  @Override
  public PlanExecution executeHttpSwitch() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpSwitchPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeHttpFork() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpForkPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpSectionPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryIgnorePlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpRetryIgnorePlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryAbortPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpRetryAbortPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRollbackPlan() {
    User user = UserThreadLocal.get();
    return orchestrationService.startExecution(customExecutionProvider.provideHttpRollbackPlan(), getAbstractions(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  @Override
  public PlanExecution executeSimpleShellScriptPlan(String accountId, String appId) {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSimpleShellScriptPlan(), getAbstractions(accountId, appId), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSimpleTimeoutPlan(String accountId, String appId) {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSimpleTimeoutPlan(), getAbstractions(accountId, appId), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeTaskChainPlanV1() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideTaskChainPlan(FacilitatorType.TASK_CHAIN), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainPlanWithFailure() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlanWithFailure(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainPlanWithNoChildren() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlanWithNoChildren(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainRollbackPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainRollbackPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testGraphPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideGraphTestPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSingleBarrierPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.providePlanWithSingleBarrier(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeMultipleBarriersPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.providePlanWithMultipleBarriers(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeResourceRestraintPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideResourceRestraintPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeResourceRestraintPlanForFunctionalTest(Plan plan, EmbeddedUser embeddedUser) {
    return orchestrationService.startExecution(plan, getAbstractions(), embeddedUser);
  }

  @Override
  public PlanExecution executeResourceRestraintWithWaitPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideResourceRestraintWithWaitPlan(), getAbstractions(), getEmbeddedUser());
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
  public PlanExecution executeSkipChildren() {
    return orchestrationService.startExecution(
        customExecutionProvider.getSkipChildrenPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public Graph getGraph(String planExecutionId) {
    return graphGenerationService.generateGraph(planExecutionId);
  }

  @Override
  public OrchestrationGraph getOrchestrationGraph(String executionPlanId) {
    return graphGenerationService.generateOrchestrationGraph(executionPlanId);
  }

  @Override
  public OrchestrationGraph getPartialOrchestrationGraph(String startingSetupNodeId, String executionPlanId) {
    return graphGenerationService.generatePartialOrchestrationGraph(startingSetupNodeId, executionPlanId);
  }

  @Override
  public void getGraphVisualization(String executionPlanId, OutputStream output) throws IOException {
    Graph graph = graphGenerationService.generateGraph(executionPlanId);
    graphVisualizer.generateImage(graph, output);
  }

  private EmbeddedUser getEmbeddedUser() {
    User user = UserThreadLocal.get();
    return EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
  }

  private Map<String, String> getAbstractions() {
    return getAbstractions(ACCOUNT_ID, APP_ID);
  }

  private Map<String, String> getAbstractions(String accountId, String appId) {
    return ImmutableMap.of("accountId", accountId, "appId", appId);
  }
}

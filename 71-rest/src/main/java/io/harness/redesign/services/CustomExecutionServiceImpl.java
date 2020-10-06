package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.dto.OrchestrationGraphDTO;
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
    return orchestrationService.startExecution(customExecutionProvider.provideHttpSwitchPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeHttpFork() {
    return orchestrationService.startExecution(customExecutionProvider.provideHttpForkPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    return orchestrationService.startExecution(customExecutionProvider.provideHttpSectionPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeRetryIgnorePlan() {
    return orchestrationService.startExecution(customExecutionProvider.provideHttpRetryIgnorePlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeRetryAbortPlan() {
    return orchestrationService.startExecution(customExecutionProvider.provideHttpRetryAbortPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeInterventionPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpInterventionPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeRollbackPlan() {
    return orchestrationService.startExecution(customExecutionProvider.provideHttpRollbackPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeSimpleShellScriptPlan(String accountId, String appId) {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSimpleShellScriptPlan(), getAbstractions(accountId, appId));
  }

  @Override
  public PlanExecution executeSimpleTimeoutPlan(String accountId, String appId) {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSimpleTimeoutPlan(), getAbstractions(accountId, appId));
  }

  @Override
  public PlanExecution executeTaskChainPlanV1() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideTaskChainPlan(FacilitatorType.TASK_CHAIN), getAbstractions());
  }

  @Override
  public PlanExecution executeSectionChainPlan() {
    return orchestrationService.startExecution(customExecutionProvider.provideSectionChainPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeSectionChainPlanWithFailure() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlanWithFailure(), getAbstractions());
  }

  @Override
  public PlanExecution executeSectionChainPlanWithNoChildren() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlanWithNoChildren(), getAbstractions());
  }

  @Override
  public PlanExecution executeSectionChainRollbackPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainRollbackPlan(), getAbstractions());
  }

  @Override
  public PlanExecution testGraphPlan() {
    return orchestrationService.startExecution(customExecutionProvider.provideGraphTestPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeSingleBarrierPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.providePlanWithSingleBarrier(), getAbstractions());
  }

  @Override
  public PlanExecution executeMultipleBarriersPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.providePlanWithMultipleBarriers(), getAbstractions());
  }

  @Override
  public PlanExecution executeResourceRestraintPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideResourceRestraintPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeResourceRestraintPlanForFunctionalTest(Plan plan, EmbeddedUser embeddedUser) {
    return orchestrationService.startExecution(plan, getAbstractions(embeddedUser));
  }

  @Override
  public PlanExecution executeResourceRestraintWithWaitPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideResourceRestraintWithWaitPlan(), getAbstractions());
  }

  @Override
  public Interrupt registerInterrupt(InterruptPackage interruptPackage) {
    return interruptManager.register(interruptPackage);
  }

  @Override
  public PlanExecution executeSkipChildren() {
    return orchestrationService.startExecution(customExecutionProvider.getSkipChildrenPlan(), getAbstractions());
  }

  @Override
  public PlanExecution executeSkipNode() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideGraphTestPlanWithSkippedNodes(), getAbstractions());
  }

  @Override
  public OrchestrationGraphDTO getOrchestrationGraph(String planExecutionId) {
    return graphGenerationService.generateOrchestrationGraph(planExecutionId);
  }

  @Override
  public OrchestrationGraphDTO getOrchestrationGraphV2(String planExecutionId) {
    return graphGenerationService.generateOrchestrationGraphV2(planExecutionId);
  }

  @Override
  public OrchestrationGraphDTO getPartialOrchestrationGraph(String startingSetupNodeId, String executionPlanId) {
    return graphGenerationService.generatePartialOrchestrationGraphFromSetupNodeId(
        startingSetupNodeId, executionPlanId);
  }

  @Override
  public OrchestrationGraphDTO getPartialOrchestrationGraphFromIdentifier(String identifier, String executionPlanId) {
    return graphGenerationService.generatePartialOrchestrationGraphFromIdentifier(identifier, executionPlanId);
  }

  @Override
  public void getGraphVisualization(String executionPlanId, OutputStream output) throws IOException {
    OrchestrationGraphDTO graph = graphGenerationService.generateOrchestrationGraph(executionPlanId);
    graphVisualizer.generateImage(graph, output);
  }

  private Map<String, String> getAbstractions() {
    return getAbstractions(ACCOUNT_ID, APP_ID);
  }

  private Map<String, String> getAbstractions(EmbeddedUser user) {
    return getAbstractions(ACCOUNT_ID, APP_ID, user);
  }

  private Map<String, String> getAbstractions(String accountId, String appId) {
    User user = UserThreadLocal.get();
    return getAbstractions(accountId, appId,
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  private Map<String, String> getAbstractions(String accountId, String appId, EmbeddedUser user) {
    return ImmutableMap.of("accountId", accountId, "appId", appId, "userId", user.getUuid(), "userName", user.getName(),
        "userEmail", user.getEmail());
  }
}

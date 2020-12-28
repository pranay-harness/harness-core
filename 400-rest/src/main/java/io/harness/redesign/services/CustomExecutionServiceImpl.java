package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.dto.OrchestrationGraphDTO;
import io.harness.engine.OrchestrationService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.PlanExecution;
import io.harness.generator.GraphVisualizer;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;
import io.harness.pms.pipeline.ExecutionTriggerInfo;
import io.harness.pms.pipeline.TriggerType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.service.GraphGenerationService;

import software.wings.beans.User;
import software.wings.security.UserThreadLocal;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@OwnedBy(CDC)
@Singleton
public class CustomExecutionServiceImpl implements CustomExecutionService {
  @Inject private OrchestrationService orchestrationService;
  @Inject private InterruptManager interruptManager;
  @Inject private GraphGenerationService graphGenerationService;
  @Inject private GraphVisualizer graphVisualizer;
  @Inject private CustomExecutionProvider customExecutionProvider;

  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String APP_ID = "d9cTupsyQjWqbhUmZ8XPdQ";

  @Override
  public PlanExecution executeHttpSwitch() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpSwitchPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeHttpFork() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpForkPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpSectionPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeRetryIgnorePlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpRetryIgnorePlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeRetryAbortPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpRetryAbortPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeInterventionPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpInterventionPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeRollbackPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideHttpRollbackPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSimpleShellScriptPlan(String accountId, String appId) {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSimpleShellScriptPlan(), getAbstractions(accountId, appId), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSimpleTimeoutPlan(String accountId, String appId) {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSimpleTimeoutPlan(), getAbstractions(accountId, appId), getTriggerInfo());
  }

  @Override
  public PlanExecution executeTaskChainPlanV1() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideTaskChainPlan(OrchestrationFacilitatorType.TASK_CHAIN), getAbstractions(),
        getTriggerInfo());
  }

  @Override
  public PlanExecution executeSectionChainPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSectionChainPlanWithFailure() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlanWithFailure(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSectionChainPlanWithNoChildren() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainPlanWithNoChildren(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSectionChainRollbackPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideSectionChainRollbackPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution testGraphPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideGraphTestPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSingleBarrierPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.providePlanWithSingleBarrier(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeMultipleBarriersPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.providePlanWithMultipleBarriers(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeResourceRestraintPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideResourceRestraintPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeResourceRestraintPlanForFunctionalTest(Plan plan, EmbeddedUser embeddedUser) {
    return orchestrationService.startExecution(plan, getAbstractions(embeddedUser), getTriggerInfo(embeddedUser));
  }

  @Override
  public PlanExecution executeResourceRestraintWithWaitPlan() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideResourceRestraintWithWaitPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public Interrupt registerInterrupt(InterruptPackage interruptPackage) {
    return interruptManager.register(interruptPackage);
  }

  @Override
  public PlanExecution executeSkipChildren() {
    return orchestrationService.startExecution(
        customExecutionProvider.getSkipChildrenPlan(), getAbstractions(), getTriggerInfo());
  }

  @Override
  public PlanExecution executeSkipNode() {
    return orchestrationService.startExecution(
        customExecutionProvider.provideGraphTestPlanWithSkippedNodes(), getAbstractions(), getTriggerInfo());
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

  private ExecutionTriggerInfo getTriggerInfo() {
    return getTriggerInfo(UserThreadLocal.get());
  }

  private ExecutionTriggerInfo getTriggerInfo(User user) {
    EmbeddedUser embeddedUser =
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build();
    return ExecutionTriggerInfo.builder().triggeredBy(embeddedUser).triggerType(TriggerType.MANUAL).build();
  }

  private ExecutionTriggerInfo getTriggerInfo(EmbeddedUser embeddedUser) {
    return ExecutionTriggerInfo.builder().triggerType(TriggerType.MANUAL).triggeredBy(embeddedUser).build();
  }
}

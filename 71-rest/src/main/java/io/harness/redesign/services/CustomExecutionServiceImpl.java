package io.harness.redesign.services;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.interrupts.ExecutionInterruptType.ABORT_ALL;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.engine.OrchestrationService;
import io.harness.engine.graph.GraphGenerationService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.exception.GeneralException;
import io.harness.execution.PlanExecution;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.interrupts.Interrupt;
import io.harness.plan.Plan;
import io.harness.presentation.Graph;
import io.harness.presentation.visualization.GraphVisualizer;
import io.harness.yaml.utils.YamlPipelineUtils;
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

  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String APP_ID = "d9cTupsyQjWqbhUmZ8XPdQ";

  @Override
  public PlanExecution executeHttpSwitch() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideHttpSwitchPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeHttpFork() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideHttpForkPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideHttpSectionPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryIgnorePlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideHttpRetryIgnorePlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRetryAbortPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideHttpRetryAbortPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeRollbackPlan() {
    User user = UserThreadLocal.get();
    return orchestrationService.startExecution(CustomExecutionUtils.provideHttpRollbackPlan(), getAbstractions(),
        EmbeddedUser.builder().uuid(user.getUuid()).email(user.getEmail()).name(user.getName()).build());
  }

  @Override
  public PlanExecution executeSimpleShellScriptPlan(String accountId, String appId) {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideSimpleShellScriptPlan(), getAbstractions(accountId, appId), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeTaskChainPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideTaskChainPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideSectionChainPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSectionChainRollbackPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideSectionChainRollbackPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testInfraState() throws IOException {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideInfraStateTestPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testGraphPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideGraphTestPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testArtifactState() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideArtifactStateTestPlan(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeSingleBarrierPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.providePlanWithSingleBarrier(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution executeMultipleBarriersPlan() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.providePlanWithMultipleBarriers(), getAbstractions(), getEmbeddedUser());
  }

  @Override
  public PlanExecution testServiceState() {
    return orchestrationService.startExecution(
        CustomExecutionUtils.provideServiceStateTestPlan(), getAbstractions(), getEmbeddedUser());
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
    return graphGenerationService.generateGraph(planExecutionId);
  }

  @Override
  public void getGraphVisualization(String executionPlanId, OutputStream output) throws IOException {
    Graph graph = graphGenerationService.generateGraph(executionPlanId);
    graphVisualizer.generateImage(graph, output);
  }

  @Override
  public PlanExecution testExecutionPlanCreator(String pipelineYaml, String accountId, String appId) {
    final CDPipeline cdPipeline;
    try {
      cdPipeline = YamlPipelineUtils.read(pipelineYaml, CDPipeline.class);
      final Plan planForPipeline = executionPlanCreatorService.createPlanForPipeline(cdPipeline, accountId);
      return orchestrationService.startExecution(
          planForPipeline, ImmutableMap.of("accountId", accountId, "appId", appId), getEmbeddedUser());
    } catch (IOException e) {
      throw new GeneralException("error while testing execution plan", e);
    }
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

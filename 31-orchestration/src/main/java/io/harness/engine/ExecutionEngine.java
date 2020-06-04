package io.harness.engine;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.execution.status.Status.RUNNING;
import static io.harness.execution.status.Status.resumableStatuses;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import io.harness.adviser.Advise;
import io.harness.adviser.Adviser;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdvisingEvent;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.Outcome;
import io.harness.delay.DelayEventHelper;
import io.harness.delegate.beans.ResponseData;
import io.harness.engine.advise.AdviseHandler;
import io.harness.engine.advise.AdviseHandlerFactory;
import io.harness.engine.executables.ExecutableInvoker;
import io.harness.engine.executables.ExecutableInvokerFactory;
import io.harness.engine.executables.InvokerPackage;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.interrupts.InterruptCheck;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.resume.EngineResumeExecutor;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorResponse;
import io.harness.facilitator.PassThroughData;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.HPersistence;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.plan.input.InputArgs;
import io.harness.registries.adviser.AdviserRegistry;
import io.harness.registries.facilitator.FacilitatorRegistry;
import io.harness.registries.resolver.ResolverRegistry;
import io.harness.registries.state.StepRegistry;
import io.harness.resolvers.Resolver;
import io.harness.state.Step;
import io.harness.state.io.FailureInfo;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepResponse;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.state.io.StepTransput;
import io.harness.waiter.WaitNotifyEngine;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Slf4j
@Redesign
@OwnedBy(CDC)
public class ExecutionEngine implements Engine {
  @Inject @Named("enginePersistence") private HPersistence hPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private Injector injector;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private StepRegistry stepRegistry;
  @Inject private AdviserRegistry adviserRegistry;
  @Inject private FacilitatorRegistry facilitatorRegistry;
  @Inject private ResolverRegistry resolverRegistry;
  @Inject private AmbianceHelper ambianceHelper;
  @Inject private EngineObtainmentHelper engineObtainmentHelper;
  @Inject private ExecutableInvokerFactory executableInvokerFactory;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private DelayEventHelper delayEventHelper;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private InterruptService interruptService;

  public PlanExecution startExecution(@Valid Plan plan, EmbeddedUser createdBy) {
    return startExecution(plan, null, createdBy);
  }

  public PlanExecution startExecution(@Valid Plan plan, InputArgs inputArgs, EmbeddedUser createdBy) {
    PlanExecution instance = PlanExecution.builder()
                                 .uuid(generateUuid())
                                 .plan(plan)
                                 .inputArgs(inputArgs == null ? new InputArgs() : inputArgs)
                                 .status(Status.RUNNING)
                                 .createdBy(createdBy)
                                 .startTs(System.currentTimeMillis())
                                 .build();
    PlanNode planNode = plan.fetchStartingNode();
    if (planNode == null) {
      logger.error("Cannot Start Execution for empty plan");
      return null;
    }
    String savedPlanExecutionId = hPersistence.save(instance);
    Ambiance ambiance = Ambiance.builder().inputArgs(inputArgs).planExecutionId(savedPlanExecutionId).build();
    triggerExecution(ambiance, planNode);
    return instance;
  }
  public void startNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    Ambiance ambiance = ambianceHelper.fetchAmbiance(nodeExecution);
    facilitateAndStartStep(ambiance, nodeExecution);
  }

  public void startNodeExecution(Ambiance ambiance) {
    NodeExecution nodeExecution = nodeExecutionService.get(ambiance.obtainCurrentRuntimeId());
    facilitateAndStartStep(ambiance, nodeExecution);
  }

  private void facilitateAndStartStep(Ambiance ambiance, NodeExecution nodeExecution) {
    try (AutoLogContext ignore = ambiance.autoLogContext()) {
      logger.info("Checking Interrupts before Node Start");
      InterruptCheck check = interruptService.checkAndHandleInterruptsBeforeNodeStart(
          ambiance.getPlanExecutionId(), ambiance.obtainCurrentRuntimeId());
      if (!check.isProceed()) {
        logger.info("Suspending Execution. Reason : {}", check.getReason());
        return;
      }
      logger.info("Proceeding with  Execution. Reason : {}", check.getReason());

      PlanNode node = nodeExecution.getNode();
      // Facilitate and execute
      List<StepTransput> inputs =
          engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
      StepParameters resolvedStepParameters =
          (StepParameters) engineExpressionService.resolve(ambiance, node.getStepParameters());
      nodeExecution = Preconditions.checkNotNull(nodeExecutionService.update(nodeExecution.getUuid(),
          ops -> setUnset(ops, NodeExecutionKeys.resolvedStepParameters, resolvedStepParameters)));
      facilitateExecution(ambiance, nodeExecution, inputs);
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  public void triggerExecution(Ambiance ambiance, PlanNode node) {
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (ambiance.obtainCurrentRuntimeId() != null) {
      previousNodeExecution = nodeExecutionService.update(
          ambiance.obtainCurrentRuntimeId(), ops -> ops.set(NodeExecutionKeys.nextId, uuid));
    }

    Ambiance cloned = reBuildAmbiance(ambiance, node, uuid);

    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(uuid)
            .node(node)
            .planExecutionId(cloned.getPlanExecutionId())
            .levels(cloned.getLevels())
            .startTs(System.currentTimeMillis())
            .status(Status.QUEUED)
            .notifyId(previousNodeExecution == null ? null : previousNodeExecution.getNotifyId())
            .parentId(previousNodeExecution == null ? null : previousNodeExecution.getParentId())
            .previousId(previousNodeExecution == null ? null : previousNodeExecution.getUuid())
            .build();
    hPersistence.save(nodeExecution);
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(cloned).executionEngine(this).build());
  }

  private Ambiance reBuildAmbiance(Ambiance ambiance, PlanNode node, String uuid) {
    Ambiance cloned = ambiance.obtainCurrentRuntimeId() == null ? ambiance : ambiance.cloneForFinish();
    cloned.addLevel(Level.fromPlanNode(uuid, node));
    return cloned;
  }

  private void facilitateExecution(Ambiance ambiance, NodeExecution nodeExecution, List<StepTransput> inputs) {
    PlanNode node = nodeExecution.getNode();
    FacilitatorResponse facilitatorResponse = null;
    for (FacilitatorObtainment obtainment : node.getFacilitatorObtainments()) {
      Facilitator facilitator = facilitatorRegistry.obtain(obtainment.getType());
      injector.injectMembers(facilitator);
      facilitatorResponse = facilitator.facilitate(
          ambiance, nodeExecution.getResolvedStepParameters(), obtainment.getParameters(), inputs);
      if (facilitatorResponse != null) {
        break;
      }
    }
    Preconditions.checkNotNull(facilitatorResponse,
        "No execution mode detected for State. Name: " + node.getName() + "Type : " + node.getStepType());
    if (facilitatorResponse.getInitialWait() != null && facilitatorResponse.getInitialWait().getSeconds() != 0) {
      FacilitatorResponse finalFacilitatorResponse = facilitatorResponse;
      // Update Status
      Preconditions.checkNotNull(
          nodeExecutionService.updateStatusWithOps(ambiance.obtainCurrentRuntimeId(), Status.WAITING,
              ops -> ops.set(NodeExecutionKeys.initialWaitDuration, finalFacilitatorResponse.getInitialWait())));
      String resumeId =
          delayEventHelper.delay(finalFacilitatorResponse.getInitialWait().getSeconds(), Collections.emptyMap());
      waitNotifyEngine.waitForAllOn(ORCHESTRATION,
          EngineWaitResumeCallback.builder()
              .ambiance(ambiance)
              .facilitatorResponse(finalFacilitatorResponse)
              .inputs(inputs)
              .build(),
          resumeId);
      return;
    }
    invokeState(ambiance, facilitatorResponse, inputs);
  }

  public void invokeState(Ambiance ambiance, FacilitatorResponse facilitatorResponse, List<StepTransput> inputs) {
    NodeExecution nodeExecution =
        Preconditions.checkNotNull(nodeExecutionService.updateStatusWithOps(ambiance.obtainCurrentRuntimeId(),
            Status.RUNNING, ops -> ops.set(NodeExecutionKeys.mode, facilitatorResponse.getExecutionMode())));
    PlanNode node = nodeExecution.getNode();
    Step currentStep = stepRegistry.obtain(node.getStepType());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(facilitatorResponse.getExecutionMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .step(currentStep)
                                 .ambiance(ambiance)
                                 .inputs(inputs)
                                 .parameters(nodeExecution.getResolvedStepParameters())
                                 .passThroughData(facilitatorResponse.getPassThroughData())
                                 .start(true)
                                 .build());
  }

  public void handleStepResponse(@NonNull String nodeExecutionId, @NonNull StepResponse stepResponse) {
    NodeExecution nodeExecution = concludeNodeExecution(nodeExecutionId, stepResponse);
    // TODO => handle before node execution update
    Ambiance ambiance = ambianceHelper.fetchAmbiance(nodeExecution);
    handleOutcomes(ambiance, stepResponse.stepOutcomeMap());

    PlanNode node = nodeExecution.getNode();
    if (isEmpty(node.getAdviserObtainments())) {
      endTransition(nodeExecution, nodeExecution.getStatus(), stepResponse);
      return;
    }
    Advise advise = null;
    for (AdviserObtainment obtainment : node.getAdviserObtainments()) {
      Adviser adviser = adviserRegistry.obtain(obtainment.getType());
      injector.injectMembers(adviser);
      advise = adviser.onAdviseEvent(AdvisingEvent.builder()
                                         .ambiance(ambiance)
                                         .outcomes(stepResponse.outcomeMap())
                                         .status(stepResponse.getStatus())
                                         .adviserParameters(obtainment.getParameters())
                                         .failureInfo(stepResponse.getFailureInfo())
                                         .build());
      if (advise != null) {
        break;
      }
    }
    if (advise == null) {
      endTransition(nodeExecution, nodeExecution.getStatus(), stepResponse);
      return;
    }
    handleAdvise(ambiance, advise);
  }

  private NodeExecution concludeNodeExecution(@NonNull String nodeExecutionId, @NonNull StepResponse stepResponse) {
    UpdateOperations<NodeExecution> operations = hPersistence.createUpdateOperations(NodeExecution.class)
                                                     .set(NodeExecutionKeys.status, stepResponse.getStatus())
                                                     .set(NodeExecutionKeys.endTs, System.currentTimeMillis());
    if (stepResponse.getFailureInfo() != null) {
      operations.set(NodeExecutionKeys.failureInfo, stepResponse.getFailureInfo());
    }
    Query<NodeExecution> query =
        hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, nodeExecutionId);
    return hPersistence.findAndModify(query, operations, HPersistence.returnNewOptions);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void handleOutcomes(Ambiance ambiance, Map<String, StepResponse.StepOutcome> stepOutcomes) {
    if (stepOutcomes == null) {
      return;
    }
    stepOutcomes.forEach((name, stepOutcome) -> {
      Outcome outcome = stepOutcome.getOutcome();
      if (outcome != null) {
        Resolver resolver = resolverRegistry.obtain(outcome.getRefType());
        resolver.consume(ambiance, name, outcome, stepOutcome.getGroup());
      }
    });
  }

  public void endTransition(NodeExecution nodeExecution, Status status, StepResponse stepResponse) {
    if (isNotEmpty(nodeExecution.getNotifyId())) {
      PlanNode planNode = nodeExecution.getNode();
      StepResponseNotifyData responseData =
          StepResponseNotifyData.builder()
              .nodeUuid(planNode.getUuid())
              .stepOutcomes(stepResponse != null ? stepResponse.getStepOutcomes() : new ArrayList<>())
              .failureInfo(stepResponse != null ? stepResponse.getFailureInfo() : null)
              .identifier(planNode.getIdentifier())
              .group(planNode.getStepType().getGroup())
              .status(status)
              .build();
      waitNotifyEngine.doneWith(nodeExecution.getNotifyId(), responseData);
    } else {
      logger.info("Ending Execution");
      planExecutionService.updateStatusWithOps(nodeExecution.getPlanExecutionId(), nodeExecution.getStatus(), null);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void handleAdvise(@NotNull Ambiance ambiance, @NotNull Advise advise) {
    AdviseHandler adviseHandler = adviseHandlerFactory.obtainHandler(advise.getType());
    adviseHandler.handleAdvise(ambiance, advise);
  }

  public void resume(String nodeInstanceId, Map<String, ResponseData> response, boolean asyncError) {
    NodeExecution nodeExecution =
        hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, nodeInstanceId).get();
    Ambiance ambiance = ambianceHelper.fetchAmbiance(nodeExecution);
    try (AutoLogContext ignore = ambiance.autoLogContext()) {
      if (!resumableStatuses().contains(nodeExecution.getStatus())) {
        logger.warn("NodeExecution is no longer in RESUMABLE state Uuid: {} Status {} ", nodeExecution.getUuid(),
            nodeExecution.getStatus());
        return;
      }
      if (nodeExecution.getStatus() != RUNNING) {
        nodeExecution = Preconditions.checkNotNull(nodeExecutionService.updateStatusWithOps(nodeInstanceId, RUNNING));
      }
      executorService.execute(EngineResumeExecutor.builder()
                                  .nodeExecution(nodeExecution)
                                  .ambiance(ambiance)
                                  .response(response)
                                  .asyncError(asyncError)
                                  .executionEngine(this)
                                  .stepRegistry(stepRegistry)
                                  .injector(injector)
                                  .build());
    } catch (Exception exception) {
      handleError(ambiance, exception);
    }
  }

  public void triggerLink(Step step, Ambiance ambiance, NodeExecution nodeExecution, PassThroughData passThroughData,
      Map<String, ResponseData> response) {
    PlanNode node = nodeExecution.getNode();
    List<StepTransput> inputs =
        engineObtainmentHelper.obtainInputs(ambiance, node.getRefObjects(), nodeExecution.getAdditionalInputs());
    ExecutableInvoker invoker = executableInvokerFactory.obtainInvoker(nodeExecution.getMode());
    invoker.invokeExecutable(InvokerPackage.builder()
                                 .step(step)
                                 .ambiance(ambiance)
                                 .inputs(inputs)
                                 .parameters(nodeExecution.getResolvedStepParameters())
                                 .responseDataMap(response)
                                 .passThroughData(passThroughData)
                                 .start(false)
                                 .build());
  }

  public void handleError(Ambiance ambiance, Exception exception) {
    try {
      StepResponse response = StepResponse.builder()
                                  .status(Status.FAILED)
                                  .failureInfo(FailureInfo.builder()
                                                   .errorMessage(ExceptionUtils.getMessage(exception))
                                                   .failureTypes(ExceptionUtils.getFailureTypes(exception))
                                                   .build())
                                  .build();
      handleStepResponse(ambiance.obtainCurrentRuntimeId(), response);
    } catch (RuntimeException ex) {
      logger.error("Error when trying to obtain the advice ", ex);
    }
  }
}
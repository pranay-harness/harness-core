package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.eraro.ErrorCode.BARRIERS_NOT_RUNNING_CONCURRENTLY;
import static io.harness.govern.Switch.unhandled;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.sm.StateType.BARRIER;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.distribution.barrier.Barrier;
import io.harness.distribution.barrier.BarrierId;
import io.harness.distribution.barrier.ForceProctor;
import io.harness.distribution.barrier.Forcer;
import io.harness.distribution.barrier.Forcer.State;
import io.harness.distribution.barrier.ForcerId;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.persistence.HIterator;
import io.harness.persistence.HKeyIterator;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.BarrierInstance;
import software.wings.beans.BarrierInstance.BarrierInstanceKeys;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.BarrierStatusData;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;

@Singleton
@Slf4j
public class BarrierServiceImpl implements BarrierService, ForceProctor {
  private static final String APP_ID = "appId";
  private static final String LEVEL = "level";

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("BarrierInstanceMonitor").poolSize(2).interval(ofMinutes(1)).build(),
        BarrierService.class,
        MongoPersistenceIterator.<BarrierInstance>builder()
            .clazz(BarrierInstance.class)
            .fieldName(BarrierInstanceKeys.nextIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .handler(this ::update)
            .filterExpander(query -> query.filter(BarrierInstanceKeys.state, STANDING.name()))
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public String save(@Valid BarrierInstance barrier) {
    return wingsPersistence.save(barrier);
  }

  @Override
  public BarrierInstance get(String barrierId) {
    return wingsPersistence.get(BarrierInstance.class, barrierId);
  }

  @Value
  @Builder
  private static class BarrierDetail {
    private String name;
    private BarrierInstance.Workflow workflow;
  }

  @Override
  public BarrierInstance update(String appId, String barrierId) {
    final BarrierInstance barrierInstance = wingsPersistence.getWithAppId(BarrierInstance.class, appId, barrierId);
    return update(barrierInstance);
  }

  @Override
  public BarrierInstance update(BarrierInstance barrierInstance) {
    if (!STANDING.name().equals(barrierInstance.getState())) {
      return barrierInstance;
    }

    // First lets try to fill up all the missing data that is available already
    final BarrierInstance.Pipeline pipeline = barrierInstance.getPipeline();
    for (int index = 0; index < pipeline.getWorkflows().size(); ++index) {
      BarrierInstance.Workflow workflow = pipeline.getWorkflows().get(index);
      if (workflow.getPipelineStageExecutionId() == null) {
        try (HKeyIterator<StateExecutionInstance> keys = new HKeyIterator(
                 wingsPersistence.createQuery(StateExecutionInstance.class)
                     .filter(StateExecutionInstanceKeys.appId, barrierInstance.getAppId())
                     .filter(StateExecutionInstanceKeys.executionUuid, pipeline.getExecutionId())
                     .filter(StateExecutionInstanceKeys.stateType, ENV_STATE.name())
                     .filter(StateExecutionInstanceKeys.pipelineStageElementId, workflow.getPipelineStageId())
                     .fetchKeys())) {
          if (!keys.hasNext()) {
            continue;
          }

          workflow.setPipelineStageExecutionId(keys.next().getId().toString());

          if (keys.hasNext()) {
            logger.error("More than one execution instance for the same pipeline state");
          }
        }
      }

      if (workflow.getWorkflowExecutionId() == null) {
        try (HKeyIterator<WorkflowExecution> keys = new HKeyIterator(
                 wingsPersistence.createQuery(WorkflowExecution.class)
                     .filter(WorkflowExecutionKeys.appId, barrierInstance.getAppId())
                     .filter(WorkflowExecutionKeys.pipelineExecutionId, pipeline.getExecutionId())
                     .filter(WorkflowExecutionKeys.executionArgs_pipelinePhaseElementId, workflow.getPipelineStageId())
                     .filter(WorkflowExecutionKeys.workflowId, workflow.getUuid())
                     .fetchKeys())) {
          if (!keys.hasNext()) {
            continue;
          }
          workflow.setWorkflowExecutionId(keys.next().getId().toString());
          if (keys.hasNext()) {
            logger.error("More than one workflow execution for the same pipeline execution");
          }
        }
      }

      if (workflow.getPhaseExecutionId() == null) {
        try (HKeyIterator<StateExecutionInstance> keys = new HKeyIterator(
                 wingsPersistence.createQuery(StateExecutionInstance.class)
                     .filter(StateExecutionInstanceKeys.appId, barrierInstance.getAppId())
                     .filter(StateExecutionInstanceKeys.executionUuid, workflow.getWorkflowExecutionId())
                     .filter(StateExecutionInstanceKeys.phaseSubWorkflowId, workflow.getPhaseUuid())
                     .field(StateExecutionInstanceKeys.stateType)
                     .in(asList(PHASE.name(), PHASE_STEP.name()))
                     .fetchKeys())) {
          if (!keys.hasNext()) {
            continue;
          }
          workflow.setPhaseExecutionId(keys.next().getId().toString());
          if (keys.hasNext()) {
            logger.error("More than one execution instance for the same phase state");
          }
        }
      }

      if (workflow.getStepExecutionId() == null) {
        try (HKeyIterator<StateExecutionInstance> keys = new HKeyIterator(
                 wingsPersistence.createQuery(StateExecutionInstance.class)
                     .filter(StateExecutionInstanceKeys.appId, barrierInstance.getAppId())
                     .filter(StateExecutionInstanceKeys.executionUuid, workflow.getWorkflowExecutionId())
                     .filter(StateExecutionInstanceKeys.stateType, BARRIER.name())
                     .filter(StateExecutionInstanceKeys.stepId, workflow.getStepUuid())
                     .fetchKeys())) {
          if (!keys.hasNext()) {
            continue;
          }
          workflow.setStepExecutionId(keys.next().getId().toString());
          if (keys.hasNext()) {
            logger.error("More than one execution instance for the same phase state");
          }
        }
      }
    }

    // Update the DB to not need to make the same queries again
    wingsPersistence.merge(barrierInstance);

    // Lets construct a barrier
    Forcer forcer = buildForcer(barrierInstance);

    Barrier barrier = Barrier.builder().id(new BarrierId(barrierInstance.getUuid())).forcer(forcer).build();
    Barrier.State state = barrier.pushDown(this);
    switch (state) {
      case STANDING:
        return barrierInstance;
      case DOWN:
        waitNotifyEngine.doneWith(barrierInstance.getUuid(), BarrierStatusData.builder().failed(false).build());
        break;
      case ENDURE:
        waitNotifyEngine.doneWith(barrierInstance.getUuid(), BarrierStatusData.builder().failed(true).build());
        break;
      default:
        unhandled(state);
    }

    barrierInstance.setState(state.name());
    wingsPersistence.merge(barrierInstance);
    return barrierInstance;
  }

  private Forcer buildForcer(BarrierInstance barrierInstance) {
    final BarrierInstance.Pipeline pipeline = barrierInstance.getPipeline();
    final String appId = barrierInstance.getAppId();
    return Forcer.builder()
        .id(new ForcerId(pipeline.getExecutionId()))
        .metadata(ImmutableMap.of(LEVEL, "pipeline", APP_ID, appId))
        .children(pipeline.getWorkflows()
                      .stream()
                      .map(workflow -> {
                        final Forcer step = Forcer.builder()
                                                .id(new ForcerId(workflow.getStepExecutionId()))
                                                .metadata(ImmutableMap.of(LEVEL, "step", APP_ID, appId))
                                                .build();
                        final Forcer phase = Forcer.builder()
                                                 .id(new ForcerId(workflow.getPhaseExecutionId()))
                                                 .metadata(ImmutableMap.of(LEVEL, "phase", APP_ID, appId))
                                                 .children(asList(step))
                                                 .build();
                        return Forcer.builder()
                            .id(new ForcerId(workflow.getPipelineStageExecutionId()))
                            .metadata(ImmutableMap.of(LEVEL, "workflow", APP_ID, appId))
                            .children(asList(phase))
                            .build();
                      })
                      .collect(toList()))
        .build();
  }

  @Override
  public Forcer.State getForcerState(ForcerId forcerId, Map<String, Object> metadata) {
    ExecutionStatus status = null;

    if ("pipeline".equals(metadata.get(LEVEL))) {
      final WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                                      .filter(WorkflowExecutionKeys.appId, metadata.get(APP_ID))
                                                      .filter(WorkflowExecutionKeys.uuid, forcerId.getValue())
                                                      .project(WorkflowExecutionKeys.status, true)
                                                      .get();
      // The barriers are created before the pipeline is triggered. This creates a window in which barrier background
      // job might trigger update while the workflow is still missing. This will happen also if we failed to trigger
      // after we created the barriers.
      if (workflowExecution == null) {
        return State.APPROACHING;
      }
      status = workflowExecution.getStatus();
    } else {
      status = wingsPersistence.createQuery(StateExecutionInstance.class)
                   .filter(StateExecutionInstanceKeys.appId, metadata.get(APP_ID))
                   .filter(StateExecutionInstanceKeys.uuid, forcerId.getValue())
                   .project(StateExecutionInstanceKeys.status, true)
                   .get()
                   .getStatus();
    }

    if (ExecutionStatus.isPositiveStatus(status)) {
      return State.ARRIVED;
    } else if (ExecutionStatus.isFinalStatus(status)) {
      return State.ABANDONED;
    }

    if ("step".equals(metadata.get(LEVEL))
        && (status == ExecutionStatus.STARTING || status == ExecutionStatus.RUNNING)) {
      return State.ARRIVED;
    }

    return State.APPROACHING;
  }

  @Override
  public String findByStep(String appId, String pipelineStageId, int pipelineStageParallelIndex,
      String workflowExecutionId, String identifier) {
    final String pipelineExecutionId = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .filter(WorkflowExecutionKeys.appId, appId)
                                           .filter(WorkflowExecutionKeys.uuid, workflowExecutionId)
                                           .project(WorkflowExecutionKeys.pipelineExecutionId, true)
                                           .get()
                                           .getPipelineExecutionId();

    try (HKeyIterator<BarrierInstance> keys =
             new HKeyIterator(wingsPersistence.createQuery(BarrierInstance.class)
                                  .filter(BarrierInstanceKeys.appId, appId)
                                  .filter(BarrierInstanceKeys.name, identifier)
                                  .filter(BarrierInstanceKeys.pipeline_executionId, pipelineExecutionId)
                                  .filter(BarrierInstanceKeys.pipeline_parallelIndex, pipelineStageParallelIndex)
                                  .filter(BarrierInstanceKeys.pipeline_workflows_pipelineStageId, pipelineStageId)
                                  .fetchKeys())) {
      if (!keys.hasNext()) {
        // We would not be able to find a barrier for if it is noop
        return null;
      }
      String result = keys.next().getId().toString();
      if (keys.hasNext()) {
        logger.error("More than one barrier for the same pipeline execution with the same identifier");
      }

      return result;
    }
  }

  @Override
  public List<BarrierInstance> obtainInstances(
      String appId, List<OrchestrationWorkflowInfo> orchestrations, String pipelineExecutionId, int parallelIndex) {
    if (isEmpty(orchestrations)) {
      return null;
    }

    final List<BarrierDetail> barrierDetails = new ArrayList<>();
    for (int i = 0; i < orchestrations.size(); ++i) {
      int concurrentTrack = i;
      final OrchestrationWorkflowInfo workflow = orchestrations.get(concurrentTrack);
      if (!(workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow)) {
        continue;
      }

      final CanaryOrchestrationWorkflow orchestrationWorkflow =
          (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();

      for (GraphNode node : orchestrationWorkflow.getPreDeploymentSteps().getSteps()) {
        BarrierDetail barrierDetail =
            barrierDetail(workflow, orchestrationWorkflow.getPreDeploymentSteps().getUuid(), node);
        if (barrierDetail != null) {
          barrierDetails.add(barrierDetail);
        }
      }

      for (GraphNode node : orchestrationWorkflow.getPostDeploymentSteps().getSteps()) {
        BarrierDetail barrierDetail =
            barrierDetail(workflow, orchestrationWorkflow.getPostDeploymentSteps().getUuid(), node);
        if (barrierDetail != null) {
          barrierDetails.add(barrierDetail);
        }
      }

      for (WorkflowPhase phase :
          ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow()).getWorkflowPhases()) {
        for (PhaseStep section : phase.getPhaseSteps()) {
          // Note that if all the steps from the same section running in
          // parallel is not critical someone still could want to do that
          // session.stepsInParallel()
          for (GraphNode node : section.getSteps()) {
            BarrierDetail barrierDetail = barrierDetail(workflow, phase.getUuid(), node);
            if (barrierDetail != null) {
              barrierDetails.add(barrierDetail);
            }
          }
        }
      }
    }

    final Map<String, List<BarrierDetail>> barriers =
        barrierDetails.stream().collect(Collectors.<BarrierDetail, String>groupingBy(BarrierDetail::getName));

    return barriers.entrySet()
        .stream()
        .map(entry -> {
          final List<BarrierDetail> value = entry.getValue();
          final long count =
              value.stream().map(details -> details.getWorkflow().getPipelineStageId()).distinct().count();

          // All items should be in different concurrentTrack
          if (count < value.size()) {
            throw new WingsException(BARRIERS_NOT_RUNNING_CONCURRENTLY);
          }
          return entry;
        })
        // Only one barrier is a noop barrier - we should just ignore it. This is completely acceptable to have -
        // if we have a pipeline scope barrier, when we execute workflow by workflow the barrier could be just one.
        // Do not ignore pipeline level barriers either, because if we have two pipeline level barrier in the same
        // workflow, we should respect them too.
        .filter(entry -> entry.getValue().size() > 1)
        .map(entry -> {
          BarrierInstance barrier =
              BarrierInstance.builder()
                  .name(entry.getKey())
                  .state(STANDING.name())
                  .pipeline(BarrierInstance.Pipeline.builder()
                                .executionId(pipelineExecutionId)
                                .parallelIndex(parallelIndex)
                                .workflows(entry.getValue().stream().map(BarrierDetail::getWorkflow).collect(toList()))
                                .build())
                  .build();
          barrier.setAppId(appId);
          return barrier;
        })
        .collect(toList());
  }

  private BarrierDetail barrierDetail(OrchestrationWorkflowInfo workflow, String phaseUuid, GraphNode node) {
    if (!BARRIER.name().equals(node.getType())) {
      return null;
    }

    return BarrierDetail.builder()
        .name((String) node.getProperties().get("identifier"))
        .workflow(BarrierInstance.Workflow.builder()
                      .uuid(workflow.getWorkflowId())
                      .pipelineStageId(workflow.getPipelineStageId())
                      .phaseUuid(phaseUuid)
                      .stepUuid(node.getId())
                      .build())
        .build();
  }

  @Override
  public void updateAllActiveBarriers(String appId) {
    try (HIterator<BarrierInstance> barrierInstances =
             new HIterator<>(wingsPersistence.createQuery(BarrierInstance.class)
                                 .filter(BarrierInstanceKeys.appId, appId)
                                 .filter(BarrierInstanceKeys.state, STANDING.name())
                                 .fetch())) {
      for (BarrierInstance barrierInstance : barrierInstances) {
        update(barrierInstance);
      }
    }
  }
}

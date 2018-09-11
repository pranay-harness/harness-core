package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.eraro.ErrorCode.BARRIERS_NOT_RUNNING_CONCURRENTLY;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.sm.StateType.BARRIER;
import static software.wings.sm.StateType.PHASE;
import static software.wings.sm.StateType.PHASE_STEP;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.distribution.barrier.Barrier;
import io.harness.distribution.barrier.BarrierId;
import io.harness.distribution.barrier.ForceProctor;
import io.harness.distribution.barrier.Forcer;
import io.harness.distribution.barrier.Forcer.State;
import io.harness.distribution.barrier.ForcerId;
import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BarrierInstance;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowPhase;
import software.wings.dl.HIterator;
import software.wings.dl.HKeyIterator;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.BarrierService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.BarrierStatusData;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionInstance;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;

@Singleton
public class BarrierServiceImpl implements BarrierService, ForceProctor {
  private static final Logger logger = LoggerFactory.getLogger(BarrierServiceImpl.class);
  private static final String APP_ID = "appId";
  private static final String LEVEL = "level";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

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
    final BarrierInstance barrierInstance = wingsPersistence.get(BarrierInstance.class, appId, barrierId);
    return update(barrierInstance);
  }

  public BarrierInstance update(BarrierInstance barrierInstance) {
    if (!STANDING.name().equals(barrierInstance.getState())) {
      return barrierInstance;
    }

    // First lets try to fill up all the missing data that is available already
    final BarrierInstance.Pipeline pipeline = barrierInstance.getPipeline();
    for (int index = 0; index < pipeline.getWorkflows().size(); ++index) {
      BarrierInstance.Workflow workflow = pipeline.getWorkflows().get(index);
      if (workflow.getPipelineStateExecutionId() == null) {
        try (HKeyIterator<StateExecutionInstance> keys = new HKeyIterator(
                 wingsPersistence.createQuery(StateExecutionInstance.class)
                     .filter(StateExecutionInstance.APP_ID_KEY, barrierInstance.getAppId())
                     .filter(StateExecutionInstance.EXECUTION_UUID_KEY, pipeline.getExecutionId())
                     .filter(StateExecutionInstance.STATE_TYPE_KEY, "ENV_STATE")
                     .filter(StateExecutionInstance.PIPELINE_STATE_ELEMENT_ID_KEY, workflow.getPipelineStateId())
                     .fetchKeys())) {
          if (!keys.hasNext()) {
            continue;
          }

          workflow.setPipelineStateExecutionId(keys.next().getId().toString());

          if (keys.hasNext()) {
            logger.error("More than one execution instance for the same pipeline state");
          }
        }
      }

      if (workflow.getWorkflowExecutionId() == null) {
        try (HKeyIterator<WorkflowExecution> keys = new HKeyIterator(
                 wingsPersistence.createQuery(WorkflowExecution.class)
                     .filter(WorkflowExecution.APP_ID_KEY, barrierInstance.getAppId())
                     .filter(WorkflowExecution.PIPELINE_EXECUTION_ID_KEY, pipeline.getExecutionId())
                     .filter(WorkflowExecution.ARGS_PIPELINE_PHASE_ELEMENT_ID_KEY, workflow.getPipelineStateId())
                     .filter(WorkflowExecution.WORKFLOW_ID_KEY, workflow.getUuid())
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
                     .filter(StateExecutionInstance.APP_ID_KEY, barrierInstance.getAppId())
                     .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflow.getWorkflowExecutionId())
                     .filter(StateExecutionInstance.PHASE_SUBWORKFLOW_ID_KEY, workflow.getPhaseUuid())
                     .field(StateExecutionInstance.STATE_TYPE_KEY)
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
                     .filter(StateExecutionInstance.APP_ID_KEY, barrierInstance.getAppId())
                     .filter(StateExecutionInstance.EXECUTION_UUID_KEY, workflow.getWorkflowExecutionId())
                     .filter(StateExecutionInstance.STATE_TYPE_KEY, BARRIER.name())
                     .filter(StateExecutionInstance.STEP_ID_KEY, workflow.getStepUuid())
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
        waitNotifyEngine.notify(barrierInstance.getUuid(), BarrierStatusData.builder().failed(false).build());
        break;
      case ENDURE:
        waitNotifyEngine.notify(barrierInstance.getUuid(), BarrierStatusData.builder().failed(true).build());
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
                            .id(new ForcerId(workflow.getPipelineStateExecutionId()))
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
                                                      .filter(WorkflowExecution.APP_ID_KEY, metadata.get(APP_ID))
                                                      .filter(WorkflowExecution.ID_KEY, forcerId.getValue())
                                                      .project(WorkflowExecution.STATUS_KEY, true)
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
                   .filter(StateExecutionInstance.APP_ID_KEY, metadata.get(APP_ID))
                   .filter(StateExecutionInstance.ID_KEY, forcerId.getValue())
                   .project(StateExecutionInstance.STATUS_KEY, true)
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
  public String findByStep(String appId, String pipelineStateId, String workflowExecutionId, String identifier) {
    final String pipelineExecutionId = wingsPersistence.createQuery(WorkflowExecution.class)
                                           .filter(WorkflowExecution.APP_ID_KEY, appId)
                                           .filter(WorkflowExecution.ID_KEY, workflowExecutionId)
                                           .project(WorkflowExecution.PIPELINE_EXECUTION_ID_KEY, true)
                                           .get()
                                           .getPipelineExecutionId();

    try (HKeyIterator<BarrierInstance> keys =
             new HKeyIterator(wingsPersistence.createQuery(BarrierInstance.class)
                                  .filter(BarrierInstance.APP_ID_KEY, appId)
                                  .filter(BarrierInstance.NAME_KEY, identifier)
                                  .filter(BarrierInstance.PIPELINE_EXECUTION_ID_KEY, pipelineExecutionId)
                                  .filter(BarrierInstance.PIPELINE_WORKFLOWS_PIPELINE_STATE_ID_KEY, pipelineStateId)
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
      String appId, List<OrchestrationWorkflowInfo> orchestrations, String pipelineExecutionId) {
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
              value.stream().map(details -> details.getWorkflow().getPipelineStateId()).distinct().count();

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
                      .pipelineStateId(workflow.getPipelineStateId())
                      .phaseUuid(phaseUuid)
                      .stepUuid(node.getId())
                      .build())
        .build();
  }

  @Override
  public void updateAllActiveBarriers() {
    try (HIterator<BarrierInstance> barrierInstances =
             new HIterator<>(wingsPersistence.createQuery(BarrierInstance.class)
                                 .filter(BarrierInstance.STATE_KEY, STANDING.name())
                                 .fetch())) {
      while (barrierInstances.hasNext()) {
        BarrierInstance barrierInstance = barrierInstances.next();
        update(barrierInstance);
      }
    }
  }

  @Override
  public void updateAllActiveBarriers(String appId) {
    try (HIterator<BarrierInstance> barrierInstances =
             new HIterator<>(wingsPersistence.createQuery(BarrierInstance.class)
                                 .filter(BarrierInstance.APP_ID_KEY, appId)
                                 .filter(BarrierInstance.STATE_KEY, STANDING.name())
                                 .fetch())) {
      while (barrierInstances.hasNext()) {
        BarrierInstance barrierInstance = barrierInstances.next();
        update(barrierInstance);
      }
    }
  }
}

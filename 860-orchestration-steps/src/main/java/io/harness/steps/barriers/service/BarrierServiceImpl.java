package io.harness.steps.barriers.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.barrier.Barrier.State;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.distribution.barrier.Barrier.builder;
import static io.harness.distribution.barrier.Forcer.State.ABANDONED;
import static io.harness.distribution.barrier.Forcer.State.APPROACHING;
import static io.harness.distribution.barrier.Forcer.State.ARRIVED;
import static io.harness.govern.Switch.unhandled;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static io.harness.pms.execution.Status.ABORTED;
import static io.harness.pms.execution.Status.EXPIRED;

import static java.time.Duration.ofMinutes;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.barrier.Barrier;
import io.harness.distribution.barrier.BarrierId;
import io.harness.distribution.barrier.ForceProctor;
import io.harness.distribution.barrier.Forcer;
import io.harness.distribution.barrier.ForcerId;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.PlanExecution;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.pms.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.repositories.BarrierNodeRepository;
import io.harness.steps.barriers.BarrierStep;
import io.harness.steps.barriers.beans.BarrierExecutionInstance;
import io.harness.steps.barriers.beans.BarrierExecutionInstance.BarrierExecutionInstanceKeys;
import io.harness.steps.barriers.beans.BarrierResponseData;
import io.harness.waiter.WaitNotifyEngine;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
@Slf4j
public class BarrierServiceImpl implements BarrierService, ForceProctor {
  private static final String LEVEL = "level";
  private static final String PLAN = "plan";
  private static final String STEP = "step";
  private static final String BARRIER_NODES_SIZE = "barrierNodesSize";
  private static final String BARRIER = "barrier";

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private BarrierNodeRepository barrierNodeRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private WaitNotifyEngine waitNotifyEngine;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("BarrierExecutionInstanceMonitor")
            .poolSize(2)
            .interval(ofMinutes(1))
            .build(),
        BarrierService.class,
        MongoPersistenceIterator.<BarrierExecutionInstance, SpringFilterExpander>builder()
            .clazz(BarrierExecutionInstance.class)
            .fieldName(BarrierExecutionInstanceKeys.nextIteration)
            .targetInterval(ofMinutes(1))
            .acceptableNoAlertDelay(ofMinutes(1))
            .handler(this::update)
            .filterExpander(
                query -> query.addCriteria(Criteria.where(BarrierExecutionInstanceKeys.barrierState).in(STANDING)))
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public BarrierExecutionInstance save(BarrierExecutionInstance barrierExecutionInstance) {
    return barrierNodeRepository.save(barrierExecutionInstance);
  }

  @Override
  public List<BarrierExecutionInstance> saveAll(List<BarrierExecutionInstance> barrierExecutionInstances) {
    return (List<BarrierExecutionInstance>) barrierNodeRepository.saveAll(barrierExecutionInstances);
  }

  @Override
  public BarrierExecutionInstance get(String barrierUuid) {
    return barrierNodeRepository.findById(barrierUuid)
        .orElseThrow(() -> new InvalidRequestException("Barrier not found for id: " + barrierUuid));
  }

  @Override
  public List<BarrierExecutionInstance> findByIdentifierAndPlanExecutionId(String identifier, String planExecutionId) {
    return barrierNodeRepository.findByIdentifierAndPlanExecutionId(identifier, planExecutionId);
  }

  @Override
  public BarrierExecutionInstance findByPlanNodeId(String planNodeId) {
    return barrierNodeRepository.findByPlanNodeId(planNodeId)
        .orElseThrow(() -> new InvalidRequestException("Barrier not found for planNodeId: " + planNodeId));
  }

  @Override
  public BarrierExecutionInstance update(BarrierExecutionInstance barrierExecutionInstance) {
    if (barrierExecutionInstance.getBarrierState() != STANDING) {
      return barrierExecutionInstance;
    }

    Forcer forcer = buildForcer(barrierExecutionInstance);

    Barrier barrier = builder().id(new BarrierId(barrierExecutionInstance.getUuid())).forcer(forcer).build();
    State state = barrier.pushDown(this);

    switch (state) {
      case STANDING:
        return barrierExecutionInstance;
      case DOWN:
        log.info("The barrier [{}] was done with", barrierExecutionInstance.getUuid());
        waitNotifyEngine.doneWith(
            barrierExecutionInstance.getBarrierGroupId(), BarrierResponseData.builder().failed(false).build());
        break;
      case ENDURE:
        waitNotifyEngine.doneWith(barrierExecutionInstance.getBarrierGroupId(),
            BarrierResponseData.builder().failed(true).errorMessage("The barrier was abandoned").build());
        break;
      case TIMED_OUT:
        waitNotifyEngine.doneWith(barrierExecutionInstance.getBarrierGroupId(),
            BarrierResponseData.builder().failed(true).errorMessage("The barrier timed out").build());
        break;
      default:
        unhandled(state);
    }

    return HPersistence.retry(() -> updateState(barrierExecutionInstance.getUuid(), state));
  }

  @Override
  public BarrierExecutionInstance updateState(String uuid, State state) {
    Query query = new Query(Criteria.where(BarrierExecutionInstanceKeys.uuid).is(uuid));
    Update update = new Update().set(BarrierExecutionInstanceKeys.barrierState, state);

    return mongoTemplate.findAndModify(query, update, BarrierExecutionInstance.class);
  }

  private Forcer buildForcer(BarrierExecutionInstance barrierExecutionInstance) {
    List<BarrierExecutionInstance> barrierInstances = barrierNodeRepository.findByIdentifierAndPlanExecutionId(
        barrierExecutionInstance.getIdentifier(), barrierExecutionInstance.getPlanExecutionId());

    return Forcer.builder()
        .id(new ForcerId(barrierExecutionInstance.getPlanExecutionId()))
        .metadata(ImmutableMap.of(LEVEL, PLAN))
        .children(Collections.singletonList(Forcer.builder()
                                                .id(new ForcerId(barrierExecutionInstance.getUuid()))
                                                .metadata(ImmutableMap.of(LEVEL, STEP, BARRIER_NODES_SIZE,
                                                    barrierInstances.size(), BARRIER, barrierExecutionInstance))
                                                .build()))
        .build();
  }

  @Override
  public Forcer.State getForcerState(ForcerId forcerId, Map<String, Object> metadata) {
    if (PLAN.equals(metadata.get(LEVEL))) {
      PlanExecution planExecution;
      try {
        planExecution = planExecutionService.get(forcerId.getValue());
      } catch (InvalidRequestException e) {
        log.error("Plan Execution was not found. State set to APPROACHING", e);
        return APPROACHING;
      }

      if (StatusUtils.positiveStatuses().contains(planExecution.getStatus())) {
        return ARRIVED;
      } else if (StatusUtils.brokeStatuses().contains(planExecution.getStatus())
          || planExecution.getStatus() == ABORTED) {
        return ABANDONED;
      }
    } else {
      int barrierExecutionInstancesSize = (Integer) metadata.get(BARRIER_NODES_SIZE);

      BarrierExecutionInstance barrierExecutionInstance = (BarrierExecutionInstance) metadata.get(BARRIER);
      List<NodeExecution> nodeExecutions = findBarrierNodesByPlanExecutionIdAndIdentifier(
          barrierExecutionInstance.getPlanExecutionId(), barrierExecutionInstance.getIdentifier());

      if (nodeExecutions.stream().anyMatch(node -> node.getStatus() == Status.SUCCEEDED)) {
        log.info("Barrier {} is down because the node is Succeeded {}", forcerId.getValue(), nodeExecutions);
        return ARRIVED;
      }
      if (nodeExecutions.stream().anyMatch(
              node -> node.getStatus() == ABORTED || node.getStatus() == Status.DISCONTINUING)) {
        log.info("Barrier {} was aborted", forcerId.getValue());
        return ABANDONED;
      }
      if (barrierExecutionInstancesSize == nodeExecutions.size()
          && nodeExecutions.stream().allMatch(node -> node.getStatus() == Status.ASYNC_WAITING)) {
        log.info(
            "Barrier {} is down because all barriers are in the ASYNC_WAITING {}", forcerId.getValue(), nodeExecutions);
        return ARRIVED;
      }
      if (nodeExecutions.stream().anyMatch(node -> node.getStatus() == EXPIRED)) {
        log.info("Barrier {} was timed out", forcerId.getValue());
        return Forcer.State.TIMED_OUT;
      }
      if (nodeExecutions.stream().anyMatch(node -> node.getStatus() == Status.FAILED)) {
        return ABANDONED;
      }
    }

    return APPROACHING;
  }

  @Override
  public List<NodeExecution> findBarrierNodesByPlanExecutionIdAndIdentifier(String planExecutionId, String identifier) {
    Query query = query(new Criteria().andOperator(where(NodeExecutionKeys.planExecutionId).is(planExecutionId),
        where("node.stepType.type").is(BarrierStep.STEP_TYPE.getType()),
        where("resolvedStepParameters.identifier").is(identifier)));
    return mongoTemplate.find(query, NodeExecution.class);
  }
}

package io.harness.engine.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.barrier.Barrier.State;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.distribution.barrier.Barrier.builder;
import static io.harness.distribution.barrier.Forcer.State.ABANDONED;
import static io.harness.distribution.barrier.Forcer.State.APPROACHING;
import static io.harness.distribution.barrier.Forcer.State.ARRIVED;
import static io.harness.govern.Switch.unhandled;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.barriers.BarrierExecutionInstance.BarrierExecutionInstanceKeys;
import io.harness.barriers.BarrierResponseData;
import io.harness.distribution.barrier.Barrier;
import io.harness.distribution.barrier.BarrierId;
import io.harness.distribution.barrier.ForceProctor;
import io.harness.distribution.barrier.Forcer;
import io.harness.distribution.barrier.ForcerId;
import io.harness.engine.executions.barrier.BarrierNodeRepository;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.persistence.HPersistence;
import io.harness.waiter.WaitNotifyEngine;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Slf4j
public class BarrierServiceImpl implements BarrierService, ForceProctor {
  private static final String LEVEL = "level";
  private static final String PLAN = "plan";
  private static final String STEP = "step";
  private static final String BARRIER_NODE_IDS = "barrierNodeIds";

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private BarrierNodeRepository barrierNodeRepository;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private SpringPersistenceProvider<BarrierExecutionInstance> persistenceProvider;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;
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
            .handler(this ::update)
            .filterExpander(
                query -> query.addCriteria(Criteria.where(BarrierExecutionInstanceKeys.barrierState).in(STANDING)))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
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
    return barrierNodeRepository.findByPlanNodeId(planNodeId);
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
        waitNotifyEngine.doneWith(
            barrierExecutionInstance.getIdentifier(), BarrierResponseData.builder().failed(false).build());
        break;
      case ENDURE:
        waitNotifyEngine.doneWith(barrierExecutionInstance.getIdentifier(),
            BarrierResponseData.builder().failed(true).errorMessage("The barrier was abandoned").build());
        break;
      case TIMED_OUT:
        waitNotifyEngine.doneWith(barrierExecutionInstance.getIdentifier(),
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
                                                .metadata(ImmutableMap.of(LEVEL, STEP, BARRIER_NODE_IDS,
                                                    BarrierNodeId.builder()
                                                        .barrierNodeIds(barrierInstances.stream()
                                                                            .map(BarrierExecutionInstance::getUuid)
                                                                            .collect(Collectors.toList()))
                                                        .build()))
                                                .build()))
        .build();
  }

  @Override
  public Forcer.State getForcerState(ForcerId forcerId, Map<String, Object> metadata) {
    if (PLAN.equals(metadata.get(LEVEL))) {
      PlanExecution planExecution = planExecutionService.get(forcerId.getValue());

      if (planExecution == null) {
        return APPROACHING;
      }

      if (Status.positiveStatuses().contains(planExecution.getStatus())) {
        return ARRIVED;
      } else if (Status.brokeStatuses().contains(planExecution.getStatus())) {
        return ABANDONED;
      }
    } else {
      List<String> barrierNodeIds = ((BarrierNodeId) metadata.get(BARRIER_NODE_IDS)).getBarrierNodeIds();
      List<NodeExecution> nodeExecutions = nodeExecutionService.findByIdIn(barrierNodeIds);
      if (nodeExecutions.stream().anyMatch(node -> node.getStatus() == Status.SUCCEEDED)) {
        return ARRIVED;
      }
      if (nodeExecutions.stream().anyMatch(
              node -> node.getStatus() == Status.ABORTED || node.getStatus() == Status.DISCONTINUING)) {
        return ABANDONED;
      }
      if (barrierNodeIds.size() == nodeExecutions.size()
          && nodeExecutions.stream().allMatch(node -> node.getStatus() == Status.ASYNC_WAITING)) {
        return ARRIVED;
      }
      if (nodeExecutions.stream().anyMatch(node -> System.currentTimeMillis() > node.getExpiryTs())) {
        return Forcer.State.TIMED_OUT;
      }
      if (nodeExecutions.stream().anyMatch(node -> node.getStatus() == Status.FAILED)) {
        return ABANDONED;
      }
    }

    return APPROACHING;
  }

  @Value
  @Builder
  private static class BarrierNodeId {
    List<String> barrierNodeIds;
  }
}

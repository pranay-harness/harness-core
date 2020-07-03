package io.harness.engine.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.barrier.Barrier.State.STANDING;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierExecutionInstance;
import io.harness.barriers.BarrierExecutionInstance.BarrierExecutionInstanceKeys;
import io.harness.engine.executions.barrier.BarrierNodeRepository;
import io.harness.exception.InvalidRequestException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;

@OwnedBy(CDC)
public class BarrierServiceImpl implements BarrierService {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private BarrierNodeRepository barrierNodeRepository;
  @Inject private SpringPersistenceProvider<BarrierExecutionInstance> persistenceProvider;

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
                query -> query.addCriteria(Criteria.where(BarrierExecutionInstanceKeys.barrierState).is(STANDING)))
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

  public BarrierExecutionInstance update(BarrierExecutionInstance barrierExecutionInstance) {
    return barrierExecutionInstance;
  }
}

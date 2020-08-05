package io.harness.steps.resourcerestraint.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.distribution.constraint.Consumer.State.ACTIVE;
import static io.harness.distribution.constraint.Consumer.State.BLOCKED;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;
import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.ExceptionLogger;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance;
import io.harness.steps.resourcerestraint.beans.ResourceRestraintInstance.ResourceRestraintInstanceKeys;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ResourceRestraintPersistenceMonitor implements Handler<ResourceRestraintInstance> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private SpringPersistenceProvider<ResourceRestraintInstance> persistenceProvider;
  @Inject private ResourceRestraintService resourceRestraintService;

  public void registerIterators() {
    PumpExecutorOptions executorOptions = PumpExecutorOptions.builder()
                                              .name("ResourceRestraintInstance-Monitor")
                                              .poolSize(10)
                                              .interval(ofSeconds(60))
                                              .build();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(executorOptions,
        ResourceRestraintPersistenceMonitor.class,
        MongoPersistenceIterator.<ResourceRestraintInstance, SpringFilterExpander>builder()
            .clazz(ResourceRestraintInstance.class)
            .fieldName(ResourceRestraintInstanceKeys.nextIteration)
            .filterExpander(q -> q.addCriteria(where(ResourceRestraintInstanceKeys.state).in(ACTIVE, BLOCKED)))
            .targetInterval(ofSeconds(30))
            .acceptableNoAlertDelay(ofSeconds(30))
            .acceptableExecutionTime(ofSeconds(30))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(ResourceRestraintInstance instance) {
    String constraintId = instance.getResourceRestraintId();
    boolean toUnblock = false;
    try {
      if (BLOCKED == instance.getState()) {
        toUnblock = true;
      } else if (ACTIVE == instance.getState()) {
        if (resourceRestraintService.updateActiveConstraintsForInstance(instance)) {
          logger.info("The following resource constraint needs to be unblocked: {}", constraintId);
          toUnblock = true;
        }
      }

      if (toUnblock) {
        // unblock the constraints
        resourceRestraintService.updateBlockedConstraints(ImmutableSet.of(constraintId));
      }

    } catch (WingsException e) {
      ExceptionLogger.logProcessedMessages(e, MANAGER, logger);
    } catch (RuntimeException e) {
      logger.error("", e);
    }
  }
}

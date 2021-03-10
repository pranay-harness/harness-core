package io.harness.accesscontrol.resources.resourcegroups;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO;
import io.harness.accesscontrol.resources.resourcegroups.persistence.ResourceGroupDBO.ResourceGroupDBOKeys;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@Singleton
@Slf4j
public class ResourceGroupReconciliationIterator implements Handler<ResourceGroupDBO> {
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final MongoTemplate mongoTemplate;
  private final HarnessResourceGroupService harnessResourceGroupService;
  private final ScopeService scopeService;
  private static final Duration reconciliationInterval = Duration.ofMinutes(10);

  @Inject
  public ResourceGroupReconciliationIterator(PersistenceIteratorFactory persistenceIteratorFactory,
      MongoTemplate mongoTemplate, HarnessResourceGroupService harnessResourceGroupService, ScopeService scopeService) {
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.mongoTemplate = mongoTemplate;
    this.harnessResourceGroupService = harnessResourceGroupService;
    this.scopeService = scopeService;
  }

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("ResourceGroupReconciliationIterator")
            .poolSize(5)
            .interval(ofSeconds(5))
            .build(),
        ResourceGroupDBO.class,
        MongoPersistenceIterator.<ResourceGroupDBO, SpringFilterExpander>builder()
            .clazz(ResourceGroupDBO.class)
            .fieldName(ResourceGroupDBOKeys.nextReconciliationIterationAt)
            .targetInterval(reconciliationInterval.plus(Duration.ofMinutes(1)))
            .acceptableNoAlertDelay(reconciliationInterval.plus(reconciliationInterval))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(ResourceGroupDBO entity) {
    harnessResourceGroupService.sync(
        entity.getIdentifier(), scopeService.buildScopeFromScopeIdentifier(entity.getScopeIdentifier()));
  }
}

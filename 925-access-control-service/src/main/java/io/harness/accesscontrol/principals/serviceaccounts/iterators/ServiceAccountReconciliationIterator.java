package io.harness.accesscontrol.principals.serviceaccounts.iterators;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.accesscontrol.commons.iterators.AccessControlIteratorsConfig;
import io.harness.accesscontrol.commons.iterators.IteratorConfig;
import io.harness.accesscontrol.principals.serviceaccounts.HarnessServiceAccountService;
import io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDBO;
import io.harness.accesscontrol.principals.serviceaccounts.persistence.ServiceAccountDBO.ServiceAccountDBOKeys;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(PL)
@Singleton
@Slf4j
public class ServiceAccountReconciliationIterator implements Handler<ServiceAccountDBO> {
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final MongoTemplate mongoTemplate;
  private final HarnessServiceAccountService harnessServiceAccountService;
  private final ScopeService scopeService;
  private final IteratorConfig iteratorConfig;

  @Inject
  public ServiceAccountReconciliationIterator(AccessControlIteratorsConfig iteratorsConfig,
      PersistenceIteratorFactory persistenceIteratorFactory, @Named("mongoTemplate") MongoTemplate mongoTemplate,
      HarnessServiceAccountService harnessServiceAccountService, ScopeService scopeService) {
    this.iteratorConfig = iteratorsConfig.getUserGroupIteratorConfig();
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.mongoTemplate = mongoTemplate;
    this.harnessServiceAccountService = harnessServiceAccountService;
    this.scopeService = scopeService;
  }

  @Override
  public void handle(ServiceAccountDBO entity) {
    harnessServiceAccountService.sync(
        entity.getIdentifier(), scopeService.buildScopeFromScopeIdentifier(entity.getScopeIdentifier()));
  }

  public void registerIterators() {
    Duration reconciliationInterval = Duration.ofSeconds(iteratorConfig.getTargetIntervalInSeconds());
    if (iteratorConfig.isEnabled()) {
      persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
          PersistenceIteratorFactory.PumpExecutorOptions.builder()
              .name("ServiceAccountReconciliationIterator")
              .poolSize(5)
              .interval(ofSeconds(5))
              .build(),
          ServiceAccountDBO.class,
          MongoPersistenceIterator.<ServiceAccountDBO, SpringFilterExpander>builder()
              .clazz(ServiceAccountDBO.class)
              .fieldName(ServiceAccountDBOKeys.nextReconciliationIterationAt)
              .targetInterval(reconciliationInterval.plus(Duration.ofMinutes(1)))
              .acceptableNoAlertDelay(reconciliationInterval.plus(reconciliationInterval))
              .handler(this)
              .schedulingType(REGULAR)
              .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
              .redistribute(true));
    }
  }
}

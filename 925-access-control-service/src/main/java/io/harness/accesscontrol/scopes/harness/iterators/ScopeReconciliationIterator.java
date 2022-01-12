/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.scopes.harness.iterators;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.accesscontrol.commons.iterators.AccessControlIteratorsConfig;
import io.harness.accesscontrol.commons.iterators.IteratorConfig;
import io.harness.accesscontrol.scopes.core.Scope;
import io.harness.accesscontrol.scopes.core.ScopeService;
import io.harness.accesscontrol.scopes.core.persistence.ScopeDBO;
import io.harness.accesscontrol.scopes.core.persistence.ScopeDBO.ScopeDBOKeys;
import io.harness.accesscontrol.scopes.harness.HarnessScopeService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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

@OwnedBy(HarnessTeam.PL)
@Singleton
@Slf4j
public class ScopeReconciliationIterator implements Handler<ScopeDBO> {
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final MongoTemplate mongoTemplate;
  private final IteratorConfig iteratorConfig;
  private final ScopeService scopeService;
  private final HarnessScopeService harnessScopeService;

  @Inject
  public ScopeReconciliationIterator(PersistenceIteratorFactory persistenceIteratorFactory, MongoTemplate mongoTemplate,
      AccessControlIteratorsConfig iteratorsConfig, ScopeService scopeService,
      HarnessScopeService harnessScopeService) {
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.mongoTemplate = mongoTemplate;
    this.iteratorConfig = iteratorsConfig.getScopeIteratorConfig();
    this.scopeService = scopeService;
    this.harnessScopeService = harnessScopeService;
  }

  public void registerIterators() {
    Duration reconciliationInterval = Duration.ofSeconds(iteratorConfig.getTargetIntervalInSeconds());
    if (iteratorConfig.isEnabled()) {
      persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
          PersistenceIteratorFactory.PumpExecutorOptions.builder()
              .name("ScopeReconciliationIterator")
              .poolSize(5)
              .interval(ofSeconds(5))
              .build(),
          ScopeDBO.class,
          MongoPersistenceIterator.<ScopeDBO, SpringFilterExpander>builder()
              .clazz(ScopeDBO.class)
              .fieldName(ScopeDBOKeys.nextReconciliationIterationAt)
              .targetInterval(reconciliationInterval.plus(Duration.ofMinutes(1)))
              .acceptableNoAlertDelay(reconciliationInterval.plus(reconciliationInterval))
              .handler(this)
              .schedulingType(REGULAR)
              .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
              .redistribute(true));
    }
  }

  @Override
  public void handle(ScopeDBO entity) {
    Scope scope = scopeService.buildScopeFromScopeIdentifier(entity.getIdentifier());
    harnessScopeService.sync(scope);
  }
}

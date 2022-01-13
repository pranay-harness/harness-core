/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.principals.users.iterators;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.accesscontrol.commons.iterators.AccessControlIteratorsConfig;
import io.harness.accesscontrol.commons.iterators.IteratorConfig;
import io.harness.accesscontrol.principals.users.HarnessUserService;
import io.harness.accesscontrol.principals.users.persistence.UserDBO;
import io.harness.accesscontrol.principals.users.persistence.UserDBO.UserDBOKeys;
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
public class UserReconciliationIterator implements Handler<UserDBO> {
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final MongoTemplate mongoTemplate;
  private final HarnessUserService harnessUserService;
  private final ScopeService scopeService;
  private final IteratorConfig iteratorConfig;

  @Inject
  public UserReconciliationIterator(AccessControlIteratorsConfig iteratorsConfig,
      PersistenceIteratorFactory persistenceIteratorFactory, @Named("mongoTemplate") MongoTemplate mongoTemplate,
      HarnessUserService harnessUserService, ScopeService scopeService) {
    this.iteratorConfig = iteratorsConfig.getUserIteratorConfig();
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.mongoTemplate = mongoTemplate;
    this.harnessUserService = harnessUserService;
    this.scopeService = scopeService;
  }

  public void registerIterators() {
    Duration reconciliationInterval = Duration.ofSeconds(iteratorConfig.getTargetIntervalInSeconds());
    if (iteratorConfig.isEnabled()) {
      persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
          PersistenceIteratorFactory.PumpExecutorOptions.builder()
              .name("UserReconciliationIterator")
              .poolSize(5)
              .interval(ofSeconds(5))
              .build(),
          UserDBO.class,
          MongoPersistenceIterator.<UserDBO, SpringFilterExpander>builder()
              .clazz(UserDBO.class)
              .fieldName(UserDBOKeys.nextReconciliationIterationAt)
              .targetInterval(reconciliationInterval.plus(Duration.ofMinutes(1)))
              .acceptableNoAlertDelay(reconciliationInterval.plus(reconciliationInterval))
              .handler(this)
              .schedulingType(REGULAR)
              .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
              .redistribute(true));
    }
  }

  @Override
  public void handle(UserDBO entity) {
    harnessUserService.sync(
        entity.getIdentifier(), scopeService.buildScopeFromScopeIdentifier(entity.getScopeIdentifier()));
  }
}

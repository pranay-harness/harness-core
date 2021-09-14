/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.scheduler;

import static io.harness.annotations.dev.HarnessModule._970_RBAC_CORE;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;

import software.wings.beans.security.AccessRequest;
import software.wings.beans.security.AccessRequest.AccessRequestKeys;
import software.wings.service.intfc.AccessRequestService;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.PL)
@TargetModule(_970_RBAC_CORE)
public class AccessRequestHandler implements Handler<AccessRequest> {
  private static final int ACCESS_REQUEST_CHECK_INTERVAL = 15;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private AccessRequestService accessRequestService;
  @Inject private MorphiaPersistenceProvider<AccessRequest> persistenceProvider;
  @Inject private AccountService accountService;
  private PersistenceIterator<PersistentIterable> accessRequestHandler;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("AccessRequestHandler")
            .poolSize(5)
            .interval(ofSeconds(5))
            .build(),
        AccessRequest.class,
        MongoPersistenceIterator.<AccessRequest, MorphiaFilterExpander<AccessRequest>>builder()
            .clazz(AccessRequest.class)
            .filterExpander(query -> query.filter(AccessRequestKeys.accessActive, Boolean.TRUE))
            .fieldName(AccessRequestKeys.nextIteration)
            .targetInterval(ofSeconds(15))
            .acceptableNoAlertDelay(ofSeconds(15))
            .acceptableExecutionTime(ofSeconds(10))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(AccessRequest accessRequest) {
    accessRequestService.checkAndUpdateAccessRequests(accessRequest);
  }
}

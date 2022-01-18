/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.capability.CapabilitySubjectPermissionCrudObserver;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.CapabilityTaskSelectionDetails.CapabilityTaskSelectionDetailsKeys;
import io.harness.capability.service.CapabilityService;
import io.harness.delegate.beans.Delegate;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;

import software.wings.service.impl.DelegateObserver;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class BlockingCapabilityPermissionsRecordHandler
    implements MongoPersistenceIterator.Handler<CapabilityTaskSelectionDetails>,
               CapabilitySubjectPermissionCrudObserver, DelegateObserver {
  private static final long CAPABILITIES_CHECK_INTERVAL_IN_SECONDS = 10L;
  private static final long MAX_PROCESSING_DURATION_MILLIS = 60000L;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<CapabilityTaskSelectionDetails> persistenceProvider;
  @Inject private HPersistence persistence;
  @Inject private DelegateService delegateService;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CapabilityService capabilityService;
  @Inject private DelegateTaskServiceClassic delegateTaskServiceClassic;

  PersistenceIterator<CapabilityTaskSelectionDetails> capSubjectPermissionIterator;

  public void registerIterators(int threadPoolSize) {
    PumpExecutorOptions options = PumpExecutorOptions.builder()
                                      .interval(Duration.ofSeconds(CAPABILITIES_CHECK_INTERVAL_IN_SECONDS))
                                      .poolSize(threadPoolSize)
                                      .name("BlockingCapabilityPermissionsRecordHandler")
                                      .build();

    capSubjectPermissionIterator = persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(options,
        CapabilityTaskSelectionDetails.class,
        MongoPersistenceIterator
            .<CapabilityTaskSelectionDetails, MorphiaFilterExpander<CapabilityTaskSelectionDetails>>builder()
            .clazz(CapabilityTaskSelectionDetails.class)
            .fieldName(CapabilityTaskSelectionDetailsKeys.blockingCheckIterations)
            .filterExpander(q -> q.field(CapabilityTaskSelectionDetailsKeys.blocked).equal(Boolean.TRUE))
            .targetInterval(Duration.ofSeconds(CAPABILITIES_CHECK_INTERVAL_IN_SECONDS))
            .acceptableNoAlertDelay(Duration.ofSeconds(80))
            .handler(this)
            .schedulingType(IRREGULAR_SKIP_MISSED)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(CapabilityTaskSelectionDetails taskSelectionDetails) {
    // do nothing
  }

  @Override
  public void onBlockingPermissionsCreated(String accountId, String delegateId) {
    if (capSubjectPermissionIterator != null) {
      capabilityService.resetDelegatePermissionCheckIterations(accountId, delegateId);

      capSubjectPermissionIterator.wakeup();
    }
  }

  @Override
  public void onReconnected(String accountId, String delegateId) {
    if (capSubjectPermissionIterator != null) {
      capabilityService.resetDelegatePermissionCheckIterations(accountId, delegateId);

      capSubjectPermissionIterator.wakeup();
    }
  }

  @Override
  public void onAdded(Delegate delegate) {
    // do nothing
  }

  @Override
  public void onDisconnected(String accountId, String delegateId) {
    // do nothing
  }
}

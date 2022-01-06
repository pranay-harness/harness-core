/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.capability.service.CapabilityService;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;

import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateCapabilitiesRecordHandler implements MongoPersistenceIterator.Handler<Delegate> {
  private static final long CAPABILITIES_CHECK_INTERVAL_IN_MINUTES = 10L;
  private static final FindOptions FETCH_LIMIT_OPTIONS = new FindOptions().limit(10);
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Delegate> persistenceProvider;
  @Inject private HPersistence persistence;
  @Inject private DelegateService delegateService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CapabilityService capabilityService;
  @Inject private DelegateTaskServiceClassic delegateTaskServiceClassic;

  public void registerIterators(int threadPoolSize) {
    PumpExecutorOptions options = PumpExecutorOptions.builder()
                                      .interval(Duration.ofMinutes(CAPABILITIES_CHECK_INTERVAL_IN_MINUTES))
                                      .poolSize(threadPoolSize)
                                      .name("DelegateCapabilitiesRecordHandler")
                                      .build();

    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(options, Delegate.class,
        MongoPersistenceIterator.<Delegate, MorphiaFilterExpander<Delegate>>builder()
            .clazz(Delegate.class)
            .fieldName(DelegateKeys.capabilitiesCheckNextIteration)
            .filterExpander(q
                -> q.field(DelegateKeys.status)
                       .equal(DelegateInstanceStatus.ENABLED)
                       .field(DelegateKeys.lastHeartBeat)
                       .greaterThan(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)))
            .targetInterval(Duration.ofMinutes(CAPABILITIES_CHECK_INTERVAL_IN_MINUTES))
            .acceptableNoAlertDelay(Duration.ofMinutes(CAPABILITIES_CHECK_INTERVAL_IN_MINUTES + 2))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(Delegate delegate) {
    // do nothing
  }
}

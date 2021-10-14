package io.harness.delegate.task.executioncapability;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionKeys;
import static io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.service.CapabilityService;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.DMSPersistence;

import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Sort;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class DelegateCapabilitiesRecordHandler implements MongoPersistenceIterator.Handler<Delegate> {
  private static final long CAPABILITIES_CHECK_INTERVAL_IN_MINUTES = 10L;
  private static final FindOptions FETCH_LIMIT_OPTIONS = new FindOptions().limit(10);
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Delegate> persistenceProvider;
  @Inject private DMSPersistence persistence;
  @Inject private DelegateService delegateService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CapabilityService capabilityService;
  @Inject private DelegateTaskServiceClassic delegateTaskServiceClassic;

  public void registerIterators() {
    PumpExecutorOptions options = PumpExecutorOptions.builder()
                                      .interval(Duration.ofMinutes(CAPABILITIES_CHECK_INTERVAL_IN_MINUTES))
                                      .poolSize(5)
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
    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())) {
      try (AutoLogContext ignore = new AccountLogContext(delegate.getAccountId(), OVERRIDE_ERROR);
           AutoLogContext ignore1 = new DelegateLogContext(delegate.getUuid(), OVERRIDE_ERROR)) {
        // Fetch all blocked capability ids to be able to exclude records that will be processed by blocking capability
        // record handler class
        Set<String> blockingCapabilityIds = capabilityService.getBlockedTaskSelectionDetails(delegate.getAccountId())
                                                .stream()
                                                .map(CapabilityTaskSelectionDetails::getCapabilityId)
                                                .collect(Collectors.toSet());

        List<CapabilitySubjectPermission> capabilitySubjectPermissions =
            persistence.createQuery(CapabilitySubjectPermission.class)
                .filter(CapabilitySubjectPermissionKeys.accountId, delegate.getAccountId())
                .filter(CapabilitySubjectPermissionKeys.delegateId, delegate.getUuid())
                .field(CapabilitySubjectPermissionKeys.revalidateAfter)
                .lessThan(System.currentTimeMillis())
                .order(Sort.ascending(CapabilitySubjectPermissionKeys.revalidateAfter))
                .asList(FETCH_LIMIT_OPTIONS)
                .stream()
                .filter(capabilitySubjectPermission
                    -> capabilitySubjectPermission.getPermissionResult() != PermissionResult.UNCHECKED
                        || !blockingCapabilityIds.contains(capabilitySubjectPermission.getCapabilityId()))
                .collect(Collectors.toList());

        if (isNotEmpty(capabilitySubjectPermissions)) {
          delegateTaskServiceClassic.executeBatchCapabilityCheckTask(
              delegate.getAccountId(), delegate.getUuid(), capabilitySubjectPermissions, null);
        } else {
          log.warn("No capability records found for delegate.");
        }
      } catch (Exception e) {
        log.error("Failed to run capabilities check task.", e);
      }
    }
  }
}

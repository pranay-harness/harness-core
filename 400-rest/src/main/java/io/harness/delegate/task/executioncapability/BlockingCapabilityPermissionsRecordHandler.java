package io.harness.delegate.task.executioncapability;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionKeys;
import static io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.IRREGULAR_SKIP_MISSED;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilitySubjectPermissionCrudObserver;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.CapabilityTaskSelectionDetails.CapabilityTaskSelectionDetailsKeys;
import io.harness.capability.service.CapabilityService;
import io.harness.delegate.beans.Delegate;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.DMSPersistence;
import io.harness.persistence.HPersistence;

import software.wings.service.impl.DelegateObserver;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

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
  @Inject private DMSPersistence persistence;
  @Inject private DelegateService delegateService;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CapabilityService capabilityService;
  @Inject private DelegateTaskServiceClassic delegateTaskServiceClassic;

  PersistenceIterator<CapabilityTaskSelectionDetails> capSubjectPermissionIterator;

  public void registerIterators() {
    PumpExecutorOptions options = PumpExecutorOptions.builder()
                                      .interval(Duration.ofSeconds(CAPABILITIES_CHECK_INTERVAL_IN_SECONDS))
                                      .poolSize(5)
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
    if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, taskSelectionDetails.getAccountId())) {
      try (AutoLogContext ignore = new AccountLogContext(taskSelectionDetails.getAccountId(), OVERRIDE_ERROR)) {
        // Get all unchecked permissions for the blocked capability. Scope for unchecked ones was just checked, prior to
        // insert
        List<CapabilitySubjectPermission> uncheckedCapabilityPermissions =
            persistence.createQuery(CapabilitySubjectPermission.class)
                .filter(CapabilitySubjectPermissionKeys.accountId, taskSelectionDetails.getAccountId())
                .filter(CapabilitySubjectPermissionKeys.capabilityId, taskSelectionDetails.getCapabilityId())
                .filter(CapabilitySubjectPermissionKeys.permissionResult, PermissionResult.UNCHECKED)
                .field(CapabilitySubjectPermissionKeys.revalidateAfter)
                .lessThan(System.currentTimeMillis())
                .order(Sort.ascending(CapabilitySubjectPermissionKeys.revalidateAfter))
                .asList();

        List<String> activeDelegates =
            assignDelegateService.retrieveActiveDelegates(taskSelectionDetails.getAccountId(), null);

        for (CapabilitySubjectPermission capabilitySubjectPermission : uncheckedCapabilityPermissions) {
          // Make sure delegate is active at the moment
          if (activeDelegates.contains(capabilitySubjectPermission.getDelegateId())) {
            // Postpone revalidateAfter to make sure record is not fetched by next iteration or some other thread
            Query<CapabilitySubjectPermission> query =
                persistence.createQuery(CapabilitySubjectPermission.class)
                    .filter(CapabilitySubjectPermissionKeys.accountId, capabilitySubjectPermission.getAccountId())
                    .filter(CapabilitySubjectPermissionKeys.uuid, capabilitySubjectPermission.getUuid());

            UpdateOperations<CapabilitySubjectPermission> updateOperations =
                persistence.createUpdateOperations(CapabilitySubjectPermission.class);
            updateOperations.set(CapabilitySubjectPermissionKeys.revalidateAfter,
                System.currentTimeMillis() + MAX_PROCESSING_DURATION_MILLIS);

            CapabilitySubjectPermission processingPermission =
                persistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);

            if (processingPermission != null) {
              delegateTaskServiceClassic.executeBatchCapabilityCheckTask(taskSelectionDetails.getAccountId(),
                  capabilitySubjectPermission.getDelegateId(), Arrays.asList(capabilitySubjectPermission),
                  taskSelectionDetails.getUuid());

              CapabilityTaskSelectionDetails updatedTaskSelectionDetails =
                  persistence.get(CapabilityTaskSelectionDetails.class, taskSelectionDetails.getUuid());
              if (!updatedTaskSelectionDetails.isBlocked()) {
                // There is at least one capable delegate. Stop processing other permission records as urgent ones
                break;
              }
            } else {
              log.warn("Processing of the capability has started by other process. Skipping...");
            }
          } else {
            log.warn("Capability check not possible, since delegate is not active at the moment. Skipping...");
          }
        }
      } catch (Exception e) {
        log.error("Failed to run capabilities check task.", e);
      }
    }
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

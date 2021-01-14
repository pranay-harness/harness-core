package io.harness.delegate.task.executioncapability;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.capability.CapabilityRequirement.CapabilityRequirementKeys;
import static io.harness.capability.CapabilitySubjectPermission.CapabilitySubjectPermissionKeys;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.IgnoreThrowable.ignoredOnPurpose;
import static io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.DelegateTask;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.persistence.HPersistence;

import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@Slf4j
public class DelegateCapabilitiesRecordHandler implements MongoPersistenceIterator.Handler<Delegate> {
  private static final long CAPABILITIES_CHECK_TASK_TIMEOUT_IN_MINUTES = 2L;
  private static final long CAPABILITIES_CHECK_INTERVAL_IN_MINUTES = 10L;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MorphiaPersistenceProvider<Delegate> persistenceProvider;
  @Inject private HPersistence persistence;
  @Inject private DelegateService delegateService;
  @Inject FeatureFlagService featureFlagService;

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
                       .equal(Status.ENABLED)
                       .field(DelegateKeys.lastHeartBeat)
                       .greaterThan(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(5)))
            .targetInterval(Duration.ofMinutes(CAPABILITIES_CHECK_INTERVAL_IN_MINUTES))
            .acceptableNoAlertDelay(Duration.ofMinutes(1))
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
        FindOptions findOptions = new FindOptions();
        findOptions.limit(10);

        List<CapabilitySubjectPermission> capabilitySubjectPermissions =
            persistence.createQuery(CapabilitySubjectPermission.class)
                .filter(CapabilitySubjectPermissionKeys.accountId, delegate.getAccountId())
                .filter(CapabilitySubjectPermissionKeys.delegateId, delegate.getUuid())
                .field(CapabilitySubjectPermissionKeys.revalidateAfter)
                .lessThan(System.currentTimeMillis())
                .order(Sort.ascending(CapabilitySubjectPermissionKeys.revalidateAfter))
                .asList(findOptions);

        if (isNotEmpty(capabilitySubjectPermissions)) {
          List<CapabilityCheckDetails> capabilityCheckDetailsList =
              capabilitySubjectPermissions.stream()
                  .map(capSubjectPermission -> {
                    // Log that we did not revalidate the capability on time
                    if (System.currentTimeMillis() > capSubjectPermission.getMaxValidUntil()) {
                      log.warn("Capability {} is being re-validated with delay of {} millis.",
                          capSubjectPermission.getCapabilityId(),
                          System.currentTimeMillis() - capSubjectPermission.getMaxValidUntil());
                    }

                    CapabilityRequirement capabilityRequirement =
                        persistence.createQuery(CapabilityRequirement.class)
                            .filter(CapabilityRequirementKeys.accountId, capSubjectPermission.getAccountId())
                            .filter(CapabilityRequirementKeys.uuid, capSubjectPermission.getCapabilityId())
                            .get();

                    if (capabilityRequirement != null && capabilityRequirement.getCapabilityParameters() != null
                        && isNotBlank(capabilityRequirement.getCapabilityType())) {
                      return CapabilityCheckDetails.builder()
                          .accountId(capSubjectPermission.getAccountId())
                          .delegateId(capSubjectPermission.getDelegateId())
                          .capabilityId(capSubjectPermission.getCapabilityId())
                          .capabilityType(CapabilityType.valueOf(capabilityRequirement.getCapabilityType()))
                          .capabilityParameters(capabilityRequirement.getCapabilityParameters())
                          .maxValidityPeriod(capabilityRequirement.getMaxValidityPeriod())
                          .revalidateAfterPeriod(capabilityRequirement.getRevalidateAfterPeriod())
                          .build();
                    }

                    return null;
                  })
                  .filter(capabilityCheckDetails -> capabilityCheckDetails != null)
                  .collect(Collectors.toList());

          if (isNotEmpty(capabilityCheckDetailsList)) {
            DelegateTask capabilitiesCheckTask =
                buildCapabilitiesCheckTask(delegate.getAccountId(), delegate.getUuid(), capabilityCheckDetailsList);

            try {
              DelegateResponseData delegateResponseData = delegateService.executeTask(capabilitiesCheckTask);

              if (delegateResponseData instanceof BatchCapabilityCheckTaskResponse) {
                BatchCapabilityCheckTaskResponse response = (BatchCapabilityCheckTaskResponse) delegateResponseData;

                for (CapabilityCheckDetails capabilityCheckDetails : response.getCapabilityCheckDetailsList()) {
                  Query<CapabilitySubjectPermission> query =
                      persistence.createQuery(CapabilitySubjectPermission.class)
                          .filter(CapabilitySubjectPermissionKeys.accountId, capabilityCheckDetails.getAccountId())
                          .filter(CapabilitySubjectPermissionKeys.delegateId, capabilityCheckDetails.getDelegateId())
                          .filter(
                              CapabilitySubjectPermissionKeys.capabilityId, capabilityCheckDetails.getCapabilityId());

                  UpdateOperations<CapabilitySubjectPermission> updateOperations =
                      persistence.createUpdateOperations(CapabilitySubjectPermission.class);
                  setUnset(updateOperations, CapabilitySubjectPermissionKeys.permissionResult,
                      capabilityCheckDetails.getPermissionResult());
                  setUnset(updateOperations, CapabilitySubjectPermissionKeys.maxValidUntil,
                      System.currentTimeMillis() + capabilityCheckDetails.getMaxValidityPeriod());
                  setUnset(updateOperations, CapabilitySubjectPermissionKeys.revalidateAfter,
                      System.currentTimeMillis() + capabilityCheckDetails.getRevalidateAfterPeriod());

                  persistence.findAndModify(query, updateOperations, HPersistence.returnNewOptions);
                }
              } else if ((delegateResponseData instanceof RemoteMethodReturnValueData)
                  && (((RemoteMethodReturnValueData) delegateResponseData).getException()
                          instanceof InvalidRequestException)) {
                log.error(
                    "Invalid request exception: ", ((RemoteMethodReturnValueData) delegateResponseData).getException());
              } else {
                log.error("Batch capabilities check task execution got unexpected delegate response {}",
                    delegateResponseData != null ? delegateResponseData.toString() : "null");
              }
            } catch (NoInstalledDelegatesException exception) {
              ignoredOnPurpose(exception);
            } catch (NoAvailableDelegatesException exception) {
              log.warn("Targeted delegate was not available for capabilities check task execution.");
            } catch (Exception e) {
              log.error("Failed to send capabilities check task for execution.", e);
            }
          }
        } else {
          log.warn("No capability records found for delegate.");
        }
      } catch (Exception e) {
        log.error("Failed to run capabilities check task.", e);
      }
    }
  }

  private DelegateTask buildCapabilitiesCheckTask(
      String accountId, String delegateId, List<CapabilityCheckDetails> capabilityCheckParamsList) {
    return DelegateTask.builder()
        .accountId(accountId)
        .rank(DelegateTaskRank.CRITICAL)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.BATCH_CAPABILITY_CHECK.name())
                  .parameters(new Object[] {BatchCapabilityCheckTaskParameters.builder()
                                                .capabilityCheckDetailsList(capabilityCheckParamsList)
                                                .build()})
                  .timeout(TimeUnit.MINUTES.toMillis(CAPABILITIES_CHECK_TASK_TIMEOUT_IN_MINUTES))
                  .build())
        .mustExecuteOnDelegateId(delegateId)
        .build();
  }
}

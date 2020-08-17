package software.wings.helpers.ext.account;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.reflect.Modifier.isAbstract;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.signup.BugsnagConstants.ACCOUNT;
import static software.wings.signup.BugsnagConstants.ENTITIES_REMAINING_FOR_DELETION;
import static software.wings.signup.BugsnagConstants.ID;
import static software.wings.signup.BugsnagConstants.JOB_NAME;
import static software.wings.signup.BugsnagConstants.STATUS;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.annotation.HarnessEntity;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.checker.rate.UsageBucket;
import io.harness.limits.checker.rate.UsageBucket.UsageBucketKeys;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.HIterator;
import io.harness.persistence.PersistentEntity;
import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.Morphia;
import org.quartz.SchedulerException;
import org.reflections.Reflections;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.BugsnagTab;
import software.wings.beans.ErrorData;
import software.wings.beans.User;
import software.wings.beans.entityinterface.ApplicationAccess;
import software.wings.beans.sso.SSOSettings;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext.SegmentGroupEventJobContextKeys;
import software.wings.service.impl.SSOSettingServiceImpl;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.signup.BugsnagErrorReporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class DeleteAccountHelper {
  private static final String ACCOUNT_ID = "accountId";
  private static final String APP_ID = "appId";
  private static final String IO_HARNESS = "io.harness";
  private static final String SOFTWARE_WINGS = "software.wings";
  private static final Set<Class<? extends PersistentEntity>> separateDeletionEntities =
      new HashSet<>(Arrays.asList(Account.class, User.class, SSOSettings.class));

  @Inject private AccountService accountService;
  @Inject private Morphia morphia;
  @Inject ServiceClassLocator serviceClassLocator;
  @Inject private SSOSettingServiceImpl ssoSettingService;
  @Inject private UserService userService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler persistentScheduler;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private BugsnagErrorReporter bugsnagErrorReporter;

  public List<String> deleteAllEntities(String accountId) {
    List<String> entitiesRemainingForDeletion = new ArrayList<>();
    entitiesRemainingForDeletion.addAll(deleteApplicationAccessEntities(accountId));
    entitiesRemainingForDeletion.addAll(deleteAccountAccessEntities(accountId));
    entitiesRemainingForDeletion.addAll(deleteOwnedByAccountEntities(accountId));
    removeAccountFromFeatureFlagsCollection(accountId);
    removeAccountFromUsageBucketsCollection(accountId);
    removeAccountFromSegmentGroupEventContextCollection(accountId);
    return entitiesRemainingForDeletion;
  }

  @VisibleForTesting
  List<String> deleteApplicationAccessEntities(String accountId) {
    List<String> remainingEntities = new ArrayList<>();
    Reflections reflections = new Reflections(SOFTWARE_WINGS, IO_HARNESS);
    Set<Class<? extends ApplicationAccess>> applicationAccessEntities =
        reflections.getSubTypesOf(ApplicationAccess.class);
    for (Class<? extends ApplicationAccess> entity : applicationAccessEntities) {
      if (!deleteEntityUsingAppId(accountId, entity)) {
        remainingEntities.add(getCollectionName(entity));
      }
    }
    return remainingEntities;
  }

  public boolean deleteEntityUsingAppId(String accountId, Class<? extends ApplicationAccess> entity) {
    if (!isAbstract(entity.getModifiers()) && !entity.isAssignableFrom(Application.class)
        && !entity.isAssignableFrom(Account.class) && PersistentEntity.class.isAssignableFrom(entity)) {
      Class<? extends PersistentEntity> persistentEntity = entity.asSubclass(PersistentEntity.class);
      return deleteAppLevelDocuments(accountId, persistentEntity);
    }
    return true;
  }

  @VisibleForTesting
  List<String> deleteAccountAccessEntities(String accountId) {
    List<String> remainingEntities = new ArrayList<>();
    Reflections reflections = new Reflections(SOFTWARE_WINGS, IO_HARNESS);
    Set<Class<? extends AccountAccess>> accountAccessEntities = reflections.getSubTypesOf(AccountAccess.class);
    for (Class<? extends AccountAccess> entity : accountAccessEntities) {
      if (!deleteEntityUsingAccountId(accountId, entity)) {
        remainingEntities.add(getCollectionName(entity));
      }
    }
    return remainingEntities;
  }

  public boolean deleteEntityUsingAccountId(String accountId, Class<? extends AccountAccess> entity) {
    String collectionName = getCollectionName(entity);
    try {
      if (!isAbstract(entity.getModifiers()) && PersistentEntity.class.isAssignableFrom(entity)) {
        logger.info("Deleting account level collection {}", collectionName);
        Class<? extends PersistentEntity> persistentEntity = entity.asSubclass(PersistentEntity.class);
        wingsPersistence.delete(
            wingsPersistence.createQuery(persistentEntity, excludeAuthority).filter(ACCOUNT_ID_KEY, accountId));
      }
    } catch (Exception e) {
      logger.error(
          "Exception while deleting AccountAccess collection {} for accountId {}", collectionName, accountId, e);
      return false;
    }
    return true;
  }

  @VisibleForTesting
  List<String> deleteOwnedByAccountEntities(String accountId) {
    List<String> remainingEntities = new ArrayList<>();
    List<OwnedByAccount> services = serviceClassLocator.descendingServicesForInterface(OwnedByAccount.class);
    for (OwnedByAccount service : services) {
      String collectionName = getCollectionName(service.getClass());
      try {
        logger.info("Deleting OwnedByAccount collection {}", collectionName);
        service.deleteByAccountId(accountId);
      } catch (Exception e) {
        logger.error(
            "Exception while deleting OwnedByAccount collection {} for accountId {}", collectionName, accountId, e);
        remainingEntities.add(collectionName);
      }
    }
    return remainingEntities;
  }

  private boolean deleteAppLevelDocuments(String accountId, Class<? extends PersistentEntity> entry) {
    try (HIterator<Application> applicationsInAccount = new HIterator<>(
             wingsPersistence.createQuery(Application.class).filter(ApplicationKeys.accountId, accountId).fetch())) {
      while (applicationsInAccount.hasNext()) {
        Application application = applicationsInAccount.next();
        wingsPersistence.delete(wingsPersistence.createQuery(entry).filter(APP_ID, application.getUuid()));
      }
    } catch (Exception e) {
      logger.error("Issue while deleting app level documents for this collection {}", entry.getName(), e);
      return false;
    }
    return true;
  }

  public boolean deleteExportableAccountData(String accountId) {
    logger.info("Deleting exportable data for account {}", accountId);
    if (accountService.get(accountId) == null) {
      throw new InvalidRequestException("The account to be deleted doesn't exist");
    }

    Set<Class<? extends PersistentEntity>> toBeExported = findExportableEntityTypes();
    logger.info("The exportable entities are {}", toBeExported);

    toBeExported.forEach(entry -> {
      try {
        deleteAppLevelDocuments(accountId, entry);
        logger.info(
            "Deleting account level documents from collection {} and count of account level records deleted are {}",
            entry.getName(), wingsPersistence.createQuery(entry).filter(ACCOUNT_ID, accountId).count());
        wingsPersistence.delete(wingsPersistence.createQuery(entry).filter(ACCOUNT_ID, accountId));
      } catch (Exception e) {
        logger.error("Issue while deleting account level documents this collection {}", entry.getName(), e);
      }
    });
    List<User> users = userService.getUsersOfAccount(accountId);
    if (!users.isEmpty()) {
      users.forEach(user -> userService.delete(accountId, user.getUuid()));
    }
    ssoSettingService.deleteByAccountId(accountId);
    return wingsPersistence.delete(Account.class, accountId);
  }

  private Set<Class<? extends PersistentEntity>> findExportableEntityTypes() {
    Set<Class<? extends PersistentEntity>> toBeExported = new HashSet<>();

    morphia.getMapper().getMappedClasses().forEach(mc -> {
      Class<?> clazz = mc.getClazz();
      if (PersistentEntity.class.isAssignableFrom(mc.getClazz()) && mc.getEntityAnnotation() != null
          && isAnnotatedExportable(clazz) && !separateDeletionEntities.contains(clazz)) {
        // Find out non-abstract classes with both 'Entity' and 'HarnessEntity' annotation.
        logger.info("Collection '{}' is exportable", clazz.getName());
        toBeExported.add(clazz.asSubclass(PersistentEntity.class));
      }
    });
    return toBeExported;
  }

  private boolean isAnnotatedExportable(Class<?> clazz) {
    HarnessEntity harnessEntity = clazz.getAnnotation(HarnessEntity.class);
    return harnessEntity != null && harnessEntity.exportable();
  }

  @VisibleForTesting
  String getCollectionName(Class<?> clazz) {
    return morphia.getMapper().getCollectionName(clazz);
  }

  public void handleMarkedForDeletion(String accountId) {
    deleteQuartzJobsForAccount(accountId);
    deletePerpetualTasksForAccount(accountId);
    List<String> entitiesRemainingForDeletion = deleteAllEntities(accountId);
    if (isEmpty(entitiesRemainingForDeletion)) {
      accountService.updateAccountStatus(accountId, AccountStatus.DELETED);
    } else {
      reportToBugsnag(accountId, entitiesRemainingForDeletion);
    }
  }

  private void deleteQuartzJobsForAccount(String accountId) {
    try {
      logger.info("Deleting all Quartz Jobs for account {}", accountId);
      persistentScheduler.deleteAllQuartzJobsForAccount(accountId);
      logger.info("Deleted all Quartz Jobs for account {}", accountId);
    } catch (SchedulerException e) {
      logger.error("Exception occurred at deleteQuartzJobsForAccount() for account {}", accountId, e);
    }
  }

  private void deletePerpetualTasksForAccount(String accountId) {
    perpetualTaskService.deleteAllTasksForAccount(accountId);
    logger.info("Deleted all Perpetual Tasks for account {}", accountId);
  }

  private void removeAccountFromFeatureFlagsCollection(String accountId) {
    featureFlagService.removeAccountReferenceFromAllFeatureFlags(accountId);
  }

  @VisibleForTesting
  void removeAccountFromUsageBucketsCollection(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(UsageBucket.class, excludeAuthority)
                                .field(UsageBucketKeys.key)
                                .contains(accountId));
  }

  @VisibleForTesting
  void removeAccountFromSegmentGroupEventContextCollection(String accountId) {
    try (HIterator<SegmentGroupEventJobContext> iterator =
             new HIterator<>(wingsPersistence.createQuery(SegmentGroupEventJobContext.class, excludeAuthority)
                                 .filter(SegmentGroupEventJobContextKeys.accountIds, accountId)
                                 .fetch())) {
      while (iterator.hasNext()) {
        SegmentGroupEventJobContext eventJobContext = iterator.next();
        eventJobContext.getAccountIds().remove(accountId);
        if (eventJobContext.getAccountIds().isEmpty()) {
          wingsPersistence.delete(eventJobContext);
        } else {
          wingsPersistence.save(eventJobContext);
        }
      }
    }
  }

  private void reportToBugsnag(String accountId, List<String> entitiesRemainingForDeletion) {
    String accountStatus = accountService.getAccountStatus(accountId);
    String message =
        String.format("Could not delete collections: [%s] for accountId: %s", entitiesRemainingForDeletion, accountId);
    logger.info(message);
    Exception exception = new GeneralException(message);
    List<BugsnagTab> bugsnagTabs = getBugsnagTabs(accountId, entitiesRemainingForDeletion, accountStatus);
    ErrorData errorData = ErrorData.builder().exception(exception).tabs(bugsnagTabs).build();
    bugsnagErrorReporter.report(errorData);
  }

  private List<BugsnagTab> getBugsnagTabs(
      String accountId, List<String> entitiesRemainingForDeletion, String accountStatus) {
    return Arrays.asList(BugsnagTab.builder().tabName(ACCOUNT).key(ID).value(accountId).build(),
        BugsnagTab.builder().tabName(ACCOUNT).key(STATUS).value(accountStatus).build(),
        BugsnagTab.builder().tabName(ACCOUNT).key(JOB_NAME).value("DeleteAccount").build(),
        BugsnagTab.builder()
            .tabName(ACCOUNT)
            .key(ENTITIES_REMAINING_FOR_DELETION)
            .value(entitiesRemainingForDeletion)
            .build());
  }
}

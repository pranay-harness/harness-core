package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AutoLogContext;
import io.harness.logging.ExceptionLogger;
import io.harness.scheduler.PersistentScheduler;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.beans.sso.LdapTestResponse.Status;
import software.wings.beans.sso.LdapUserResponse;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.features.LdapFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.logcontext.LdapGroupSyncJobLogContext;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Periodic job which syncs the LDAP group users with the linked group in Harness for given SSO provider
 *
 * @author Swapnil
 */
@Slf4j
public class LdapGroupSyncJob implements Job {
  private static final SecureRandom random = new SecureRandom();
  private static final String SSO_PROVIDER_ID_KEY = "ssoId";

  public static final String GROUP = "LDAP_GROUP_SYNC_CRON_JOB";
  private static final int POLL_INTERVAL = 900; // Seconds

  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private SSOService ssoService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private UserService userService;
  @Inject private UserGroupService userGroupService;
  @Inject private AccountService accountService;
  @Inject @Named(LdapFeature.FEATURE_NAME) private PremiumFeature ldapFeature;

  public static void addWithDelay(PersistentScheduler jobScheduler, String accountId, String ssoId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + random.nextInt((int) TimeUnit.SECONDS.toMillis(POLL_INTERVAL));
    addInternal(jobScheduler, accountId, ssoId, new Date(startTime));
  }

  public static void add(PersistentScheduler jobScheduler, String accountId, String ssoId) {
    addInternal(jobScheduler, accountId, ssoId, null);
  }

  private static void addInternal(
      PersistentScheduler jobScheduler, String accountId, String ssoId, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(LdapGroupSyncJob.class)
                        .withIdentity(ssoId, GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .usingJobData(SSO_PROVIDER_ID_KEY, ssoId)
                        .build();

    TriggerBuilder triggerBuilder =
        TriggerBuilder.newTrigger()
            .withIdentity(ssoId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever());
    if (triggerStartTime != null) {
      triggerBuilder.startAt(triggerStartTime);
    }

    jobScheduler.ensureJob__UnderConstruction(job, triggerBuilder.build());
  }

  public static void delete(
      PersistentScheduler jobScheduler, SSOSettingService ssoSettingService, String accountId, String ssoId) {
    jobScheduler.deleteJob(ssoId, GROUP);
    ssoSettingService.closeSyncFailureAlertIfOpen(accountId, ssoId);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    try {
      String accountId = jobExecutionContext.getMergedJobDataMap().getString(ACCOUNT_ID_KEY);
      String ssoId = jobExecutionContext.getMergedJobDataMap().getString(SSO_PROVIDER_ID_KEY);
      LdapSettings settings = ssoSettingService.getLdapSettingsByUuid(ssoId);
      if (settings == null) {
        jobScheduler.deleteJob(ssoId, GROUP);
        return;
      }
      // The app level lock was a work around for the threading issue we observed in quartz scheduler. The execute() was
      // getting called on all the managers. Its supposed to call it only on one manager. This is a way to stop that
      // from happening.
      try (AcquiredLock lock = persistentLocker.tryToAcquireLock(LdapSettings.class, ssoId, Duration.ofSeconds(60))) {
        if (lock == null) {
          return;
        }
        executorService.submit(() -> executeInternal(accountId, ssoId));
      }
    } catch (WingsException exception) {
      // do nothing. Only one manager should acquire the lock.
    } catch (Exception e) {
      // Catching all exceptions to prevent immediate job retry.
      logger.error("Error while trying to sync user group for ldap sso provider", e);
    }
  }

  private void updateRemovedGroupMembers(UserGroup userGroup, Collection<LdapUserResponse> expectedMembers,
      Map<UserGroup, Set<User>> removedGroupMembers) {
    if (isEmpty(userGroup.getMembers())) {
      return;
    }

    Set<String> expectedMemberEmails =
        expectedMembers.stream().map(LdapUserResponse::getEmail).filter(Objects::nonNull).collect(Collectors.toSet());

    Set<User> removedUsers = userGroup.getMembers()
                                 .stream()
                                 .filter(member -> !expectedMemberEmails.contains(member.getEmail()))
                                 .collect(Collectors.toSet());

    if (!removedGroupMembers.containsKey(userGroup)) {
      removedGroupMembers.put(userGroup, Sets.newHashSet());
    }
    removedGroupMembers.getOrDefault(userGroup, Sets.newHashSet()).addAll(removedUsers);
  }

  private void updateAddedGroupMembers(UserGroup userGroup, Collection<LdapUserResponse> expectedMembers,
      Map<LdapUserResponse, Set<UserGroup>> addedGroupMembers) {
    Set<String> existingUserEmails;
    if (isEmpty(userGroup.getMembers())) {
      existingUserEmails = Sets.newHashSet();
    } else {
      existingUserEmails = userGroup.getMembers().stream().map(User::getEmail).collect(Collectors.toSet());
    }

    expectedMembers.stream()
        .filter(member -> member.getEmail() != null && !existingUserEmails.contains(member.getEmail()))
        .forEach(member -> {
          if (!addedGroupMembers.containsKey(member)) {
            addedGroupMembers.put(member, Sets.newHashSet());
          }
          addedGroupMembers.get(member).add(userGroup);
        });
  }

  @VisibleForTesting
  UserGroup syncUserGroupMetadata(UserGroup userGroup, LdapGroupResponse groupResponse) {
    userGroup.setSsoGroupName(groupResponse.getName());
    return userGroupService.save(userGroup);
  }

  private void syncUserGroupMembers(String accountId, Map<UserGroup, Set<User>> removedGroupMembers,
      Map<LdapUserResponse, Set<UserGroup>> addedGroupMembers) {
    Map<String, String> emailToName = new HashMap<>();
    createEmailToNameMap(emailToName, addedGroupMembers.keySet());
    createEmailToNameMap(emailToName, removedGroupMembers.values());
    Map<String, Set<UserGroup>> emailToUserGroups =
        getUserGroupsByEmailMap(accountId, addedGroupMembers, removedGroupMembers);
    emailToUserGroups.forEach((email, userGroups) -> {
      UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                  .withAccountId(accountId)
                                  .withEmail(email)
                                  .withName(emailToName.get(email))
                                  .withUserGroups(Lists.newArrayList(userGroups))
                                  .build();
      userService.inviteUserNew(userInvite);
    });
  }

  private void createEmailToNameMap(Map<String, String> emailToNameMap, Collection<Set<User>> collection) {
    collection.forEach(users -> users.forEach(user -> emailToNameMap.put(user.getEmail(), user.getName())));
  }

  private void createEmailToNameMap(Map<String, String> emailToNameMap, Set<LdapUserResponse> collection) {
    collection.forEach(ldapUserResponse -> emailToNameMap.put(ldapUserResponse.getEmail(), ldapUserResponse.getName()));
  }

  @VisibleForTesting
  Map<String, Set<UserGroup>> getUserGroupsByEmailMap(
      String accountId, Map<LdapUserResponse, Set<UserGroup>> addedGroups, Map<UserGroup, Set<User>> removedGroups) {
    Set<User> usersSet = new HashSet<>();
    for (Set<User> userSet : removedGroups.values()) {
      usersSet.addAll(userSet);
    }
    Map<String, Set<UserGroup>> emailToUserGroups = new HashMap<>();
    addedGroups.forEach((member, userGroups) -> {
      emailToUserGroups.computeIfAbsent(member.getEmail(), k -> new HashSet<>()).addAll(userGroups);
      User user = userService.getUserByEmail(member.getEmail());
      if (user != null) {
        usersSet.add(user);
      }
    });
    userService.loadUserGroupsForUsers(new ArrayList<>(usersSet), accountId);
    usersSet.forEach(user -> {
      if (emailToUserGroups.containsKey(user.getEmail())) {
        emailToUserGroups.get(user.getEmail()).addAll(user.getUserGroups());
      }
    });
    removedGroups.forEach((userGroup, users) -> users.forEach(user -> {
      emailToUserGroups.computeIfAbsent(user.getEmail(), k -> new HashSet<>(user.getUserGroups()));
      emailToUserGroups.get(user.getEmail()).remove(userGroup);
    }));

    return emailToUserGroups;
  }

  @VisibleForTesting
  LdapGroupResponse fetchGroupDetails(
      LdapSettings ldapSettings, EncryptedDataDetail encryptedDataDetail, UserGroup userGroup) {
    SyncTaskContext syncTaskContext = SyncTaskContext.builder()
                                          .accountId(ldapSettings.getAccountId())
                                          .appId(GLOBAL_APP_ID)
                                          .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                          .build();

    LdapGroupResponse groupResponse = delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
                                          .fetchGroupByDn(ldapSettings, encryptedDataDetail, userGroup.getSsoGroupId());
    if (null == groupResponse) {
      String message = String.format(LdapConstants.USER_GROUP_SYNC_INVALID_REMOTE_GROUP, userGroup.getName());
      logger.info("LDAP : Group Response from delegate is null");
      throw new WingsException(ErrorCode.USER_GROUP_SYNC_FAILURE, message);
    }
    logger.info("LDAP : Group Response from delegate {}", groupResponse);
    return groupResponse;
  }

  @VisibleForTesting
  boolean validateUserGroupStates(Collection<UserGroup> userGroups) {
    for (UserGroup userGroup : userGroups) {
      UserGroup savedUserGroup = userGroupService.get(userGroup.getAccountId(), userGroup.getUuid(), false);
      if (!savedUserGroup.isSsoLinked()) {
        return false;
      }
      if (!savedUserGroup.getSsoGroupId().equals(userGroup.getSsoGroupId())) {
        return false;
      }
    }
    return true;
  }

  @VisibleForTesting
  void syncUserGroups(String accountId, LdapSettings ldapSettings, List<UserGroup> userGroups, String ssoId) {
    Map<UserGroup, Set<User>> removedGroupMembers = new HashMap<>();
    Map<LdapUserResponse, Set<UserGroup>> addedGroupMembers = new HashMap<>();

    EncryptedDataDetail encryptedDataDetail = ldapSettings.getEncryptedDataDetails(secretManager);

    List<UserGroup> userGroupsFailedToSync = new ArrayList<>();
    for (UserGroup userGroup : userGroups) {
      try {
        LdapGroupResponse groupResponse = fetchGroupDetails(ldapSettings, encryptedDataDetail, userGroup);

        if (!groupResponse.isSelectable()) {
          String message = String.format(
              LdapConstants.USER_GROUP_SYNC_NOT_ELIGIBLE, userGroup.getName(), groupResponse.getMessage());
          throw new UnsupportedOperationException(message);
        }

        userGroup = syncUserGroupMetadata(userGroup, groupResponse);

        updateRemovedGroupMembers(userGroup, groupResponse.getUsers(), removedGroupMembers);
        updateAddedGroupMembers(userGroup, groupResponse.getUsers(), addedGroupMembers);
      } catch (Exception e) {
        logger.error("LDAP sync failed for userGroup {}", userGroup.getName(), e);
        userGroupsFailedToSync.add(userGroup);
      }
    }

    if (userGroupsFailedToSync.isEmpty()) {
      ssoSettingService.closeSyncFailureAlertIfOpen(accountId, ssoId);
    } else {
      String userGroupsFailed =
          userGroupsFailedToSync.stream().map(UserGroup::getName).collect(Collectors.joining(", ", "[", "]"));
      ssoSettingService.raiseSyncFailureAlert(
          accountId, ssoId, String.format("Ldap Sync failed for groups: %s", userGroupsFailed));
    }
    // Sync the groups only if the state is still the same as we started. Else any change in the groups would have
    // already triggered another cron job and it will handle it.
    if (validateUserGroupStates(userGroups)) {
      syncUserGroupMembers(accountId, removedGroupMembers, addedGroupMembers);
    }
  }

  private void executeInternal(String accountId, String ssoId) {
    if (!ldapFeature.isAvailableForAccount(accountId)) {
      logger.info("Skipping LDAP sync. ssoId {} accountId {}", ssoId, accountId);
      return;
    }

    LdapSettings ldapSettings = ssoSettingService.getLdapSettingsByUuid(ssoId);
    if (ldapSettings == null) {
      jobScheduler.deleteJob(ssoId, GROUP);
      ssoSettingService.closeSyncFailureAlertIfOpen(accountId, ssoId);
      return;
    }

    try (AutoLogContext ignore = new LdapGroupSyncJobLogContext(accountId, ssoId, OVERRIDE_ERROR)) {
      logger.info("Executing ldap group sync job for ssoId: {} and accountId: {}", ssoId, accountId);

      LdapTestResponse ldapTestResponse = ssoService.validateLdapConnectionSettings(ldapSettings, accountId);
      if (ldapTestResponse.getStatus() == Status.FAILURE) {
        if (ssoSettingService.isDefault(accountId, ssoId)) {
          ssoSettingService.sendSSONotReachableNotification(accountId, ldapSettings);
        } else {
          ssoSettingService.raiseSyncFailureAlert(
              accountId, ssoId, String.format(LdapConstants.SSO_PROVIDER_NOT_REACHABLE, ldapSettings.getDisplayName()));
        }
        return;
      }

      List<UserGroup> userGroupsToSync = userGroupService.getUserGroupsBySsoId(accountId, ssoId);
      syncUserGroups(accountId, ldapSettings, userGroupsToSync, ssoId);

      logger.info("Ldap group sync job done for ssoId {} accountId {}", ssoId, accountId);
    } catch (WingsException exception) {
      if (exception.getCode() == ErrorCode.USER_GROUP_SYNC_FAILURE) {
        ssoSettingService.raiseSyncFailureAlert(accountId, ssoId, exception.getMessage());
      } else {
        ssoSettingService.raiseSyncFailureAlert(accountId, ssoId,
            String.format(LdapConstants.USER_GROUP_SYNC_FAILED, ldapSettings.getDisplayName())
                + exception.getMessage());
      }
      ExceptionLogger.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception ex) {
      ssoSettingService.raiseSyncFailureAlert(accountId, ssoId,
          String.format(LdapConstants.USER_GROUP_SYNC_FAILED, ldapSettings.getDisplayName()) + ex.getMessage());
      logger.error("Error while syncing ssoId {} in accountId {}", ssoId, accountId, ex);
    }
  }
}

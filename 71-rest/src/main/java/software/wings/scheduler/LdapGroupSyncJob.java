package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.DelegateTask.SyncTaskContext;
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
import software.wings.exception.WingsExceptionMapper;
import software.wings.helpers.ext.ldap.LdapConstants;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.ldap.LdapDelegateService;
import software.wings.service.intfc.security.SecretManager;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Periodic job which syncs the LDAP group users with the linked group in Harness for given SSO provider
 *
 * @author Swapnil
 */
public class LdapGroupSyncJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(LdapGroupSyncJob.class);

  public static final String ACCOUNT_ID_KEY = "accountId";
  private static final String SSO_PROVIDER_ID_KEY = "ssoId";

  public static final String GROUP = "LDAP_GROUP_SYNC_CRON_JOB";
  private static final int POLL_INTERVAL = 600; // Seconds

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ExecutorService executorService;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private SSOService ssoService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private UserService userService;
  @Inject private UserGroupService userGroupService;

  public static void add(QuartzScheduler jobScheduler, String accountId, String ssoId) {
    jobScheduler.deleteJob(ssoId, GROUP);

    JobDetail job = JobBuilder.newJob(LdapGroupSyncJob.class)
                        .withIdentity(ssoId, GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .usingJobData(SSO_PROVIDER_ID_KEY, ssoId)
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(ssoId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  public static void delete(
      QuartzScheduler jobScheduler, SSOSettingService ssoSettingService, String accountId, String ssoId) {
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
      logger.warn("Error while trying to sync user group for ldap sso provider", e);
    }
  }

  private void updateRemovedGroupMembers(UserGroup userGroup, Collection<LdapUserResponse> expectedMembers,
      Map<UserGroup, Set<User>> removedGroupMembers) {
    if (isEmpty(userGroup.getMembers())) {
      return;
    }

    Set<String> expectedMemberEmails =
        expectedMembers.stream().map(LdapUserResponse::getEmail).collect(Collectors.toSet());

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

    expectedMembers.stream().filter(member -> !existingUserEmails.contains(member.getEmail())).forEach(member -> {
      if (!addedGroupMembers.containsKey(member)) {
        addedGroupMembers.put(member, Sets.newHashSet());
      }
      addedGroupMembers.get(member).add(userGroup);
    });
  }

  private UserGroup syncUserGroupMetadata(UserGroup userGroup, LdapGroupResponse groupResponse) {
    userGroup.setSsoGroupName(groupResponse.getName());
    return userGroupService.save(userGroup);
  }

  private void syncUserGroupMembers(String accountId, Map<UserGroup, Set<User>> removedGroupMembers,
      Map<LdapUserResponse, Set<UserGroup>> addedGroupMembers) {
    removedGroupMembers.forEach((userGroup, users) -> userGroupService.removeMembers(userGroup, users, false));

    addedGroupMembers.forEach((member, userGroups) -> {
      UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                  .withAccountId(accountId)
                                  .withEmail(member.getEmail())
                                  .withName(member.getName())
                                  .withUserGroups(Lists.newArrayList(userGroups))
                                  .build();
      userService.inviteUser(userInvite);
    });
  }

  private LdapGroupResponse fetchGroupDetails(
      LdapSettings ldapSettings, EncryptedDataDetail encryptedDataDetail, UserGroup userGroup) {
    SyncTaskContext syncTaskContext =
        aContext().withAccountId(ldapSettings.getAccountId()).withAppId(Base.GLOBAL_APP_ID).build();

    LdapGroupResponse groupResponse = delegateProxyFactory.get(LdapDelegateService.class, syncTaskContext)
                                          .fetchGroupByDn(ldapSettings, encryptedDataDetail, userGroup.getSsoGroupId());
    if (null == groupResponse) {
      String message = String.format(LdapConstants.USER_GROUP_SYNC_INVALID_REMOTE_GROUP, userGroup.getName());
      throw new WingsException(ErrorCode.USER_GROUP_SYNC_FAILURE, message);
    }

    return groupResponse;
  }

  private boolean validateUserGroupStates(Collection<UserGroup> userGroups) {
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

  private void syncUserGroups(String accountId, LdapSettings ldapSettings, List<UserGroup> userGroups) {
    Map<UserGroup, Set<User>> removedGroupMembers = new HashMap<>();
    Map<LdapUserResponse, Set<UserGroup>> addedGroupMembers = new HashMap<>();

    EncryptedDataDetail encryptedDataDetail = ldapSettings.getEncryptedDataDetails(secretManager);

    for (UserGroup userGroup : userGroups) {
      LdapGroupResponse groupResponse = fetchGroupDetails(ldapSettings, encryptedDataDetail, userGroup);

      if (!groupResponse.isSelectable()) {
        String message =
            String.format(LdapConstants.USER_GROUP_SYNC_NOT_ELIGIBLE, userGroup.getName(), groupResponse.getMessage());
        throw new WingsException(ErrorCode.USER_GROUP_SYNC_FAILURE, message);
      }

      userGroup = syncUserGroupMetadata(userGroup, groupResponse);

      updateRemovedGroupMembers(userGroup, groupResponse.getUsers(), removedGroupMembers);
      updateAddedGroupMembers(userGroup, groupResponse.getUsers(), addedGroupMembers);
    }

    // Sync the groups only if the state is still the same as we started. Else any change in the groups would have
    // already triggered another cron job and it will handle it.
    if (validateUserGroupStates(userGroups)) {
      syncUserGroupMembers(accountId, removedGroupMembers, addedGroupMembers);
    }
  }

  private void executeInternal(String accountId, String ssoId) {
    try {
      ssoSettingService.closeSyncFailureAlertIfOpen(accountId, ssoId);
      logger.info("Executing ldap group sync job for ssoId: {}", ssoId);

      LdapSettings ldapSettings = ssoSettingService.getLdapSettingsByUuid(ssoId);
      if (ldapSettings == null) {
        jobScheduler.deleteJob(ssoId, GROUP);
        return;
      }

      LdapTestResponse ldapTestResponse = ssoService.validateLdapConnectionSettings(ldapSettings, accountId);
      if (ldapTestResponse.getStatus().equals(Status.FAILURE)) {
        if (ssoSettingService.isDefault(accountId, ssoId)) {
          ssoSettingService.sendSSONotReachableNotification(accountId, ldapSettings);
        } else {
          ssoSettingService.raiseSyncFailureAlert(
              accountId, ssoId, String.format(LdapConstants.SSO_PROVIDER_NOT_REACHABLE, ldapSettings.getDisplayName()));
        }
        return;
      }

      List<UserGroup> userGroupsToSync = userGroupService.getUserGroupsBySsoId(accountId, ssoId);
      syncUserGroups(accountId, ldapSettings, userGroupsToSync);

      ssoSettingService.closeSyncFailureAlertIfOpen(accountId, ssoId);

      logger.info("Ldap group sync job done for ssoId:" + ssoId);
    } catch (WingsException exception) {
      if (exception.getCode().equals(ErrorCode.USER_GROUP_SYNC_FAILURE)) {
        ssoSettingService.raiseSyncFailureAlert(accountId, ssoId, exception.getMessage());
      } else {
        ssoSettingService.raiseSyncFailureAlert(
            accountId, ssoId, String.format(LdapConstants.USER_GROUP_SYNC_FAILED, ssoId) + exception.getMessage());
      }
      WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
    } catch (Exception ex) {
      ssoSettingService.raiseSyncFailureAlert(
          accountId, ssoId, String.format(LdapConstants.USER_GROUP_SYNC_FAILED, ssoId) + ex.getMessage());
      logger.warn("Error while syncing ssoId: {}", ssoId, ex);
    }
  }
}

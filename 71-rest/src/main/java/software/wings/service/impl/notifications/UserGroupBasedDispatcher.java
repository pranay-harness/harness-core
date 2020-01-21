package software.wings.service.impl.notifications;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Notification;
import software.wings.beans.User;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.UserService;

import java.util.List;

public class UserGroupBasedDispatcher implements NotificationDispatcher<UserGroup> {
  private static final Logger log = LoggerFactory.getLogger(UserGroupBasedDispatcher.class);

  @Inject private NotificationSetupService notificationSetupService;
  @Inject private EmailDispatcher emailDispatcher;
  @Inject private SlackMessageDispatcher slackMessageDispatcher;
  @Inject private PagerDutyEventDispatcher pagerDutyEventDispatcher;
  @Inject private UserService userService;
  @Inject private AccountService accountService;

  @Override
  public void dispatch(List<Notification> notifications, UserGroup userGroup) {
    if (isEmpty(notifications)) {
      return;
    }

    if (null == userGroup.getNotificationSettings()) {
      log.info("Notification Settings is null for User Group. No message will be sent. userGroup={} accountId={}",
          userGroup.getName(), userGroup.getAccountId());
      return;
    }

    log.info("User group to notify. id={} name={}", userGroup.getUuid(), userGroup.getName());
    NotificationSettings notificationSettings = userGroup.getNotificationSettings();
    String accountId = notifications.get(0).getAccountId();

    // if `isUseIndividualEmails` is true, then notify all "members" of group
    if (notificationSettings.isUseIndividualEmails()) {
      List<String> emails =
          userGroup.getMembers().stream().filter(User::isEmailVerified).map(User::getEmail).collect(toList());

      log.info("[isUseIndividualEmails=true] Dispatching notifications to all the users of userGroup. uuid={} name={}",
          userGroup.getUuid(), userGroup.getName());
      emailDispatcher.dispatch(notifications, emails);
    }

    List<String> emailAddresses = userGroup.getEmailAddresses();
    if (CollectionUtils.isNotEmpty(emailAddresses)) {
      try {
        log.info("Sending emails to these addresses: {}", emailAddresses);
        emailDispatcher.dispatch(notifications, emailAddresses);
      } catch (Exception e) {
        log.error("Error sending emails to these addresses: {}", emailAddresses, e);
      }
    }

    // recommended to *not* log Slack Webhook urls and pager duty keys.
    // see discussion here: https://harness.slack.com/archives/C838QA2CW/p1562774945009400

    if (null != userGroup.getSlackConfig()) {
      try {
        log.info("Trying to send slack message. slack configuration: {}", userGroup.getSlackConfig());
        slackMessageDispatcher.dispatch(notifications, userGroup.getSlackConfig());
      } catch (Exception e) {
        log.error("Error sending slack message. Slack Config: {}", userGroup.getSlackConfig(), e);
      }
    }

    boolean isCommunityAccount = accountService.isCommunityAccount(accountId);
    if (isCommunityAccount) {
      log.info("Pager duty Configuration will be ignored since it's a community account. accountId={}", accountId);
      return;
    }

    if (EmptyPredicate.isNotEmpty(userGroup.getPagerDutyIntegrationKey())) {
      try {
        log.info("Trying to send pager duty event. userGroupId={} accountId={}", userGroup.getUuid(), accountId);
        pagerDutyEventDispatcher.dispatch(accountId, notifications, userGroup.getPagerDutyIntegrationKey());
      } catch (Exception e) {
        log.error("Error sending pager duty event. userGroupId={} accountId={}", userGroup.getUuid(), accountId, e);
      }
    }
  }

  @Override
  public EmailDispatcher getEmailDispatcher() {
    return emailDispatcher;
  }

  @Override
  public SlackMessageDispatcher getSlackDispatcher() {
    return slackMessageDispatcher;
  }

  @Override
  public Logger logger() {
    return log;
  }
}

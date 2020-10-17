package software.wings.service.impl.notifications;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.ERROR;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import software.wings.beans.FailureNotification;
import software.wings.beans.Notification;
import software.wings.beans.User;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.security.UserGroup;
import software.wings.dl.WingsPersistence;
import software.wings.processingcontrollers.NotificationProcessingController;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.UserService;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Slf4j
public class UserGroupBasedDispatcher implements NotificationDispatcher<UserGroup> {
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private EmailDispatcher emailDispatcher;
  @Inject private SlackMessageDispatcher slackMessageDispatcher;
  @Inject private PagerDutyEventDispatcher pagerDutyEventDispatcher;
  @Inject private MicrosoftTeamsMessageDispatcher microsoftTeamsMessageDispatcher;
  @Inject private UserService userService;
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private NotificationProcessingController notificationProcessingController;

  @Override
  public void dispatch(List<Notification> notifications, UserGroup userGroup) {
    if (isEmpty(notifications)) {
      return;
    }

    if (!notificationProcessingController.canProcessAccount(userGroup.getAccountId())) {
      logger.info("User Group's {} account {} is disabled. Notifications cannot be dispatched", userGroup.getUuid(),
          userGroup.getAccountId());
      return;
    }

    if (null == userGroup.getNotificationSettings()) {
      logger.info("Notification Settings is null for User Group. No message will be sent. userGroup={} accountId={}",
          userGroup.getName(), userGroup.getAccountId());
      return;
    }

    logger.info("User group to notify. id={} name={}", userGroup.getUuid(), userGroup.getName());
    NotificationSettings notificationSettings = userGroup.getNotificationSettings();
    String accountId = notifications.get(0).getAccountId();

    // if `isUseIndividualEmails` is true, then notify all "members" of group
    if (notificationSettings.isUseIndividualEmails()) {
      List<String> emails =
          userGroup.getMembers().stream().filter(User::isEmailVerified).map(User::getEmail).collect(toList());

      logger.info(
          "[isUseIndividualEmails=true] Dispatching notifications to all the users of userGroup. uuid={} name={}",
          userGroup.getUuid(), userGroup.getName());
      emailDispatcher.dispatch(notifications, emails);
    }

    List<String> emailAddresses = userGroup.getEmailAddresses();
    if (CollectionUtils.isNotEmpty(emailAddresses)) {
      try {
        logger.info("Sending emails to these addresses: {}", emailAddresses);
        emailDispatcher.dispatch(notifications, emailAddresses);
      } catch (Exception e) {
        logger.error("Error sending emails to these addresses: {}", emailAddresses, e);
      }
    }

    // recommended to *not* log Slack Webhook urls and pager duty keys.
    // see discussion here: https://harness.slack.com/archives/C838QA2CW/p1562774945009400

    if (null != userGroup.getSlackConfig()) {
      for (Notification notification : notifications) {
        if (notification instanceof FailureNotification) {
          fetchErrorMessages(notification);
        }
      }

      try {
        logger.info("Trying to send slack message. slack configuration: {}", userGroup.getSlackConfig());
        slackMessageDispatcher.dispatch(notifications, userGroup.getSlackConfig());
      } catch (Exception e) {
        logger.error("Error sending slack message. Slack Config: {}", userGroup.getSlackConfig(), e);
      }
    }

    if (EmptyPredicate.isNotEmpty(userGroup.getMicrosoftTeamsWebhookUrl())) {
      try {
        logger.info(
            "Trying to send message to Microsoft Teams. userGroupId={} accountId={}", userGroup.getUuid(), accountId);
        microsoftTeamsMessageDispatcher.dispatch(notifications, userGroup.getMicrosoftTeamsWebhookUrl());
      } catch (Exception e) {
        logger.error(
            "Error sending message to Microsoft Teams. userGroupId={} accountId={}", userGroup.getUuid(), accountId, e);
      }
    }

    boolean isCommunityAccount = accountService.isCommunityAccount(accountId);
    if (isCommunityAccount) {
      logger.info("Pager duty Configuration will be ignored since it's a community account. accountId={}", accountId);
      return;
    }

    if (EmptyPredicate.isNotEmpty(userGroup.getPagerDutyIntegrationKey())) {
      try {
        logger.info("Trying to send pager duty event. userGroupId={} accountId={}", userGroup.getUuid(), accountId);
        pagerDutyEventDispatcher.dispatch(accountId, notifications, userGroup.getPagerDutyIntegrationKey());
      } catch (Exception e) {
        logger.error("Error sending pager duty event. userGroupId={} accountId={}", userGroup.getUuid(), accountId, e);
      }
    }
  }

  private void fetchErrorMessages(Notification notification) {
    Query<StateExecutionInstance> query =
        wingsPersistence.createQuery(StateExecutionInstance.class)
            .filter(StateExecutionInstanceKeys.appId, notification.getAppId())
            .filter(StateExecutionInstanceKeys.executionUuid, notification.getEntityId())
            .order(Sort.ascending(StateExecutionInstanceKeys.endTs));

    List<Criteria> criterias = new ArrayList<>();
    criterias.add(query.criteria("status").equal(FAILED));
    criterias.add(query.criteria("status").equal(ERROR));
    query.or(criterias.toArray(new Criteria[0]));

    List<StateExecutionInstance> allExecutionInstances = query.asList();

    HashSet<String> parentInstances = new HashSet<>();
    for (StateExecutionInstance stateExecutionInstance : allExecutionInstances) {
      if (stateExecutionInstance.getStateType().equals("PHASE")) {
        notification.getNotificationTemplateVariables().replace(
            "FAILED_PHASE", "", "*" + stateExecutionInstance.getDisplayName() + " failed*\n");
      }
      parentInstances.add(stateExecutionInstance.getParentInstanceId());
    }

    Map<String, String> errorMap = new LinkedHashMap<>();

    for (StateExecutionInstance stateExecutionInstance : allExecutionInstances) {
      if (!parentInstances.contains(stateExecutionInstance.getUuid())) {
        String errorMessage =
            stateExecutionInstance.getStateExecutionMap().get(stateExecutionInstance.getStateName()).getErrorMsg();
        if (errorMessage != null && !errorMessage.equals("")) {
          errorMap.put(stateExecutionInstance.getDisplayName() + " failed -", errorMessage);
        } else {
          errorMap.put(stateExecutionInstance.getDisplayName() + " failed", "");
        }
      }
    }

    notification.getNotificationTemplateVariables().put("ERRORS", generateErrorMessageString(errorMap));

    if (errorMap.size() > 3) {
      notification.getNotificationTemplateVariables().put("MORE_ERRORS", (errorMap.size() - 3) + " more errors");
      notification.getNotificationTemplateVariables().put(
          "ERROR_URL", notification.getNotificationTemplateVariables().get("WORKFLOW_URL"));
    }
  }

  private String generateErrorMessageString(Map<String, String> errorMap) {
    StringBuilder errorMsgString = new StringBuilder();
    int counter = 0;
    for (Map.Entry error : errorMap.entrySet()) {
      errorMsgString.append('*');
      errorMsgString.append(error.getKey());
      errorMsgString.append("* ");
      errorMsgString.append(error.getValue());
      errorMsgString.append('\n');
      if (++counter > 2) {
        break;
      }
    }
    return errorMsgString.toString();
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
    return logger;
  }
}

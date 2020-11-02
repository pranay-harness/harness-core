package software.wings.service.impl.notifications;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import software.wings.beans.Notification;
import software.wings.beans.NotificationGroup;
import software.wings.beans.User;
import software.wings.processingcontrollers.NotificationProcessingController;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.UserService;

import java.util.List;

@Slf4j
public class NotificationGroupBasedDispatcher implements NotificationDispatcher<NotificationGroup> {
  @Inject private NotificationSetupService notificationSetupService;
  @Inject private EmailDispatcher emailDispatcher;
  @Inject private SlackMessageDispatcher slackMessageDispatcher;
  @Inject private UserService userService;
  @Inject private NotificationProcessingController notificationProcessingController;

  @Override
  public void dispatch(List<Notification> notifications, NotificationGroup notificationGroup) {
    if (isEmpty(notifications) || null == notificationGroup) {
      return;
    }

    if (!notificationProcessingController.canProcessAccount(notificationGroup.getAccountId())) {
      log.info("Notification Group's {} account {} is disabled. Notifications cannot be dispatched",
          notificationGroup.getUuid(), notificationGroup.getAccountId());
      return;
    }

    String appId = notifications.get(0).getAppId();
    notificationGroup = notificationSetupService.readNotificationGroup(appId, notificationGroup.getUuid());
    handleNotificationGroupRoles(notificationGroup, notifications);
    iterateOverAddressesAndNotifiy(notifications, notificationGroup);
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

  private void handleNotificationGroupRoles(NotificationGroup notificationGroup, List<Notification> notifications) {
    if (null == notificationGroup || CollectionUtils.isEmpty(notificationGroup.getRoles())) {
      return;
    }

    notificationGroup.getRoles().forEach(role -> {
      PageRequest<User> request = aPageRequest()
                                      .withLimit(PageRequest.UNLIMITED)
                                      .addFilter("appId", Operator.EQ, notificationGroup.getAppId())
                                      .addFilter("roles", Operator.IN, role)
                                      .addFieldsIncluded("email", "emailVerified")
                                      .build();

      PageResponse<User> users = userService.list(request, false);
      List<String> toAddresses = users.stream().filter(User::isEmailVerified).map(User::getEmail).collect(toList());
      log.info("Dispatching notifications to all the users of role {}", role.getRoleType().getDisplayName());
      emailDispatcher.dispatch(notifications, toAddresses);
    });
  }
}

package software.wings.service.impl.notifications;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.FailureNotification;
import software.wings.beans.Notification;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.AccountService;

import java.util.Collections;
import java.util.List;

public class UserGroupBasedDispatcherTest extends WingsBaseTest {
  @Mock private SlackMessageDispatcher slackMessageDispatcher;
  @Mock private AccountService accountService;
  @Mock private EmailDispatcher emailDispatcher;

  @InjectMocks @Inject @UseUserGroup private NotificationDispatcher<UserGroup> userGroupDispatcher;

  @Test
  @Category(UnitTests.class)
  public void dispatch_shouldSkipSlackForCommunity() {
    String accountId = "some-account-id";
    Notification notification = FailureNotification.Builder.aFailureNotification()
                                    .withNotificationTemplateId("some-template-id")
                                    .withAccountId(accountId)
                                    .withAppId("some-app-id")
                                    .build();

    List<String> emails = Collections.singletonList("noreply@harness.io");
    SlackNotificationSetting slackConfig = new SlackNotificationSetting("#josh-chan", "some-webhook-url");
    UserGroup userGroup = UserGroup.builder()
                              .name("some-name")
                              .description("some-desc")
                              .accountId(accountId)
                              .notificationSettings(new NotificationSettings(false, false, emails, slackConfig, ""))
                              .build();

    // verify slack message is sent
    List<Notification> notifications = Collections.singletonList(notification);
    userGroupDispatcher.dispatch(notifications, userGroup);
    Mockito.verify(slackMessageDispatcher).dispatch(notifications, slackConfig);
    Mockito.verify(emailDispatcher).dispatch(notifications, emails);

    // verify slack message is NOT sent
    Mockito.when(accountService.isCommunityAccount(accountId)).thenReturn(true);
    userGroupDispatcher.dispatch(notifications, userGroup);
    Mockito.verifyNoMoreInteractions(slackMessageDispatcher);
    Mockito.verify(emailDispatcher, Mockito.times(2)).dispatch(notifications, emails);
  }
}

package software.wings.service.impl.notifications;

import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.UserGroup;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.service.intfc.NotificationSetupService;

import java.util.Collections;
import java.util.List;

public class NotificationDispatcherTest extends WingsBaseTest {
  @Inject
  @InjectMocks
  @UseNotificationGroup
  private NotificationDispatcher<NotificationGroup> notificationGroupDispatcher;

  @Inject @InjectMocks @UseUserGroup private NotificationDispatcher<UserGroup> userGroupNotificationDispatcher;

  @Mock private NotificationSetupService notificationSetupService;
  @Mock private NotificationMessageResolver notificationMessageResolver;
  @Mock private EmailDispatcher emailDispatcher;
  @Mock private SlackMessageDispatcher slackDispatcher;

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void testNotificationGroupBasedDispatcher() {
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");
    List<String> slackChannels = Lists.newArrayList("#some-channel");

    NotificationGroup notificationGroup = aNotificationGroup()
                                              .withUuid(NOTIFICATION_GROUP_ID)
                                              .withAppId(APP_ID)
                                              .addAddressesByChannelType(NotificationChannelType.EMAIL, toAddresses)
                                              .addAddressesByChannelType(NotificationChannelType.SLACK, slackChannels)
                                              .build();

    when(notificationSetupService.readNotificationGroup(APP_ID, NOTIFICATION_GROUP_ID)).thenReturn(notificationGroup);

    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setBody(ENTITY_CREATE_NOTIFICATION.name());
    emailTemplate.setSubject(ENTITY_CREATE_NOTIFICATION.name());
    when(notificationMessageResolver.getEmailTemplate(ENTITY_CREATE_NOTIFICATION.name())).thenReturn(emailTemplate);

    InformationNotification notification = InformationNotification.builder()
                                               .accountId(ACCOUNT_ID)
                                               .appId(APP_ID)
                                               .entityId(WORKFLOW_EXECUTION_ID)
                                               .entityType(ORCHESTRATED_DEPLOYMENT)
                                               .notificationTemplateId(ENTITY_CREATE_NOTIFICATION.name())
                                               .notificationTemplateVariables(ImmutableMap.of("WORKFLOW_NAME",
                                                   WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "DATE", "DATE"))
                                               .build();

    List<Notification> notifications = Collections.singletonList(notification);
    notificationGroupDispatcher.dispatch(notifications, notificationGroup);
    verify(emailDispatcher).dispatch(notifications, toAddresses);
    verify(slackDispatcher).dispatch(notifications, slackChannels);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void testUserGroupBasedDispatcher() {
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");

    SlackNotificationSetting slackConfig = SlackNotificationSetting.emptyConfig();
    NotificationSettings settings = new NotificationSettings(true, true, toAddresses, slackConfig, "");
    UserGroup userGroup = UserGroup.builder()
                              .uuid(NOTIFICATION_GROUP_ID)
                              .appId(APP_ID)
                              .accountId("some-account-id")
                              .notificationSettings(settings)
                              .build();

    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setBody(ENTITY_CREATE_NOTIFICATION.name());
    emailTemplate.setSubject(ENTITY_CREATE_NOTIFICATION.name());
    when(notificationMessageResolver.getEmailTemplate(ENTITY_CREATE_NOTIFICATION.name())).thenReturn(emailTemplate);

    InformationNotification notification = InformationNotification.builder()
                                               .accountId(ACCOUNT_ID)
                                               .appId(APP_ID)
                                               .entityId(WORKFLOW_EXECUTION_ID)
                                               .entityType(ORCHESTRATED_DEPLOYMENT)
                                               .notificationTemplateId(ENTITY_CREATE_NOTIFICATION.name())
                                               .notificationTemplateVariables(ImmutableMap.of("WORKFLOW_NAME",
                                                   WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "DATE", "DATE"))
                                               .build();

    List<Notification> notifications = Collections.singletonList(notification);
    userGroupNotificationDispatcher.dispatch(notifications, userGroup);
    verify(emailDispatcher).dispatch(notifications, toAddresses);
    verify(slackDispatcher).dispatch(notifications, slackConfig);
  }
}
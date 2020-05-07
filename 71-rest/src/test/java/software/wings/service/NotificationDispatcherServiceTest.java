package software.wings.service;

import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
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
import io.harness.persistence.HQuery;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationBatch;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.UserGroup;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.service.impl.notifications.NotificationDispatcher;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.UserGroupService;

import java.util.Collections;
import java.util.List;

public class NotificationDispatcherServiceTest extends WingsBaseTest {
  @Inject @InjectMocks private NotificationDispatcherService notificationDispatcherService;

  @Mock private UserGroupService userGroupService;
  @Mock private NotificationMessageResolver notificationMessageResolver;

  @Mock private NotificationDispatcher<NotificationGroup> notificationGroupDispatcher;
  @Mock private NotificationDispatcher<UserGroup> userGroupNotificationDispatcher;

  @Mock private HQuery<NotificationBatch> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations<NotificationBatch> updateOperations;

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldTriggerNotificationGroupDispatcher() {
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");

    String accountId = "some-account-id";
    NotificationGroup notificationGroup = aNotificationGroup()
                                              .withUuid(NOTIFICATION_GROUP_ID)
                                              .withAppId(APP_ID)
                                              .withAccountId(accountId)
                                              .addAddressesByChannelType(NotificationChannelType.EMAIL, toAddresses)
                                              .build();

    NotificationRule notificationRule = aNotificationRule().addNotificationGroup(notificationGroup).build();

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

    notificationDispatcherService.dispatchNotification(notification, Collections.singletonList(notificationRule));
    verifyZeroInteractions(userGroupService);
    verifyZeroInteractions(userGroupNotificationDispatcher);

    verify(notificationGroupDispatcher).dispatch(Collections.singletonList(notification), notificationGroup);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldTriggerUserGroupDispatcher() {
    String accountId = "some-account-id";

    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");
    SlackNotificationSetting slackConfig = SlackNotificationSetting.emptyConfig();
    NotificationSettings settings = new NotificationSettings(true, true, toAddresses, slackConfig, "", "");

    List<String> userGroupIds = Collections.singletonList("first");
    UserGroup userGroup = UserGroup.builder().accountId(accountId).notificationSettings(settings).build();
    NotificationRule notificationRule = aNotificationRule().withUserGroupIds(userGroupIds).build();

    when(userGroupService.get(Mockito.eq(accountId), Mockito.anyString())).thenReturn(userGroup);

    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setBody(ENTITY_CREATE_NOTIFICATION.name());
    emailTemplate.setSubject(ENTITY_CREATE_NOTIFICATION.name());
    when(notificationMessageResolver.getEmailTemplate(ENTITY_CREATE_NOTIFICATION.name())).thenReturn(emailTemplate);

    InformationNotification notification = InformationNotification.builder()
                                               .accountId(accountId)
                                               .appId(APP_ID)
                                               .entityId(WORKFLOW_EXECUTION_ID)
                                               .entityType(ORCHESTRATED_DEPLOYMENT)
                                               .notificationTemplateId(ENTITY_CREATE_NOTIFICATION.name())
                                               .notificationTemplateVariables(ImmutableMap.of("WORKFLOW_NAME",
                                                   WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "DATE", "DATE"))
                                               .build();

    notificationDispatcherService.dispatchNotification(notification, Collections.singletonList(notificationRule));
    verifyZeroInteractions(notificationGroupDispatcher);
    verify(userGroupNotificationDispatcher).dispatch(Collections.singletonList(notification), userGroup);
  }
}

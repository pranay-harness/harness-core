package software.wings.service;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.SlackConfig.Builder.aSlackConfig;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACTS;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.persistence.HQuery;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationBatch;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.WingsTestConstants;

import java.util.Collections;
import java.util.List;

/**
 * Created by rishi on 10/31/16.
 */
public class NotificationDispatcherServiceTest extends WingsBaseTest {
  @Inject @InjectMocks private NotificationDispatcherService notificationDispatcherService;

  @Mock private NotificationSetupService notificationSetupService;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private SlackNotificationService slackNotificationService;
  @Mock private SettingsService settingsService;
  @Mock private NotificationMessageResolver notificationMessageResolver;

  @Mock private HQuery<NotificationBatch> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations<NotificationBatch> updateOperations;

  @Test
  public void shouldDispatchEmailNotification() {
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");

    NotificationGroup notificationGroup = aNotificationGroup()
                                              .withUuid(NOTIFICATION_GROUP_ID)
                                              .withAppId(APP_ID)
                                              .addAddressesByChannelType(NotificationChannelType.EMAIL, toAddresses)
                                              .build();

    when(notificationSetupService.readNotificationGroup(APP_ID, NOTIFICATION_GROUP_ID)).thenReturn(notificationGroup);
    NotificationRule notificationRule = aNotificationRule().addNotificationGroup(notificationGroup).build();

    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setBody(ENTITY_CREATE_NOTIFICATION.name());
    emailTemplate.setSubject(ENTITY_CREATE_NOTIFICATION.name());
    when(notificationMessageResolver.getEmailTemplate(ENTITY_CREATE_NOTIFICATION.name())).thenReturn(emailTemplate);

    InformationNotification notification = anInformationNotification()
                                               .withAccountId(ACCOUNT_ID)
                                               .withAppId(APP_ID)
                                               .withEntityId(WORKFLOW_EXECUTION_ID)
                                               .withEntityType(ORCHESTRATED_DEPLOYMENT)
                                               .withNotificationTemplateId(ENTITY_CREATE_NOTIFICATION.name())
                                               .withNotificationTemplateVariables(ImmutableMap.of("WORKFLOW_NAME",
                                                   WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "DATE", "DATE"))
                                               .build();

    notificationDispatcherService.dispatchNotification(notification, asList(notificationRule));
    verify(emailNotificationService)
        .sendAsync(EmailData.builder()
                       .to(toAddresses)
                       .cc(Collections.emptyList())
                       .subject(ENTITY_CREATE_NOTIFICATION.name())
                       .body(ENTITY_CREATE_NOTIFICATION.name())
                       .system(true)
                       .build());
  }

  @Test
  public void shouldDispatchSlackNotification() {
    when(notificationMessageResolver.getSlackTemplate(ENTITY_CREATE_NOTIFICATION.name()))
        .thenReturn(ENTITY_CREATE_NOTIFICATION.name());

    List<String> channels = asList("#channel1", "#channel2");
    NotificationGroup notificationGroup = aNotificationGroup()
                                              .withUuid(NOTIFICATION_GROUP_ID)
                                              .withAppId(APP_ID)
                                              .addAddressesByChannelType(NotificationChannelType.SLACK, channels)
                                              .build();
    NotificationRule notificationRule = aNotificationRule().addNotificationGroup(notificationGroup).build();
    when(notificationSetupService.readNotificationGroup(APP_ID, NOTIFICATION_GROUP_ID)).thenReturn(notificationGroup);

    InformationNotification notification =
        anInformationNotification()
            .withAccountId(ACCOUNT_ID)
            .withAppId(APP_ID)
            .withEntityId(WORKFLOW_EXECUTION_ID)
            .withEntityType(ORCHESTRATED_DEPLOYMENT)
            .withNotificationTemplateId(ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(ImmutableMap.of(
                "WORKFLOW_NAME", WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "ARTIFACTS", ARTIFACTS, "DATE", "DATE"))
            .build();

    SlackConfig slackConfig = aSlackConfig().withOutgoingWebhookUrl(WingsTestConstants.PORTAL_URL).build();
    when(settingsService.getGlobalSettingAttributesByType(ACCOUNT_ID, SettingVariableTypes.SLACK.name()))
        .thenReturn(asList(SettingAttribute.Builder.aSettingAttribute().withValue(slackConfig).build()));

    notificationDispatcherService.dispatchNotification(notification, asList(notificationRule));
    channels.forEach(channel
        -> verify(slackNotificationService)
               .sendMessage(slackConfig, channel, HARNESS_NAME, ENTITY_CREATE_NOTIFICATION.name()));
  }
}

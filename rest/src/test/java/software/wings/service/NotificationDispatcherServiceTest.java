package software.wings.service;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationBatch.Builder.aNotificationBatch;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.beans.SlackConfig.Builder.aSlackConfig;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_FAILED_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACTS;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_BATCH_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_GROUP_ID;
import static software.wings.utils.WingsTestConstants.NOTIFICATION_RULE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionScope;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationBatch;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.dl.WingsPersistence;
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
  @Mock private WingsPersistence wingsPersistence;

  @Mock private Query<NotificationBatch> query;
  @Mock private FieldEnd end;
  @Mock private UpdateOperations<NotificationBatch> updateOperations;

  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(NotificationBatch.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);

    when(wingsPersistence.createUpdateOperations(NotificationBatch.class)).thenReturn(updateOperations);
    when(updateOperations.set(anyString(), any())).thenReturn(updateOperations);
    when(updateOperations.addToSet(anyString(), any())).thenReturn(updateOperations);
  }

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
  public void shouldNotSendNotificationIfNotLastMessageInTheBatch() {
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");

    NotificationGroup notificationGroup = aNotificationGroup()
                                              .withUuid(NOTIFICATION_GROUP_ID)
                                              .withAppId(APP_ID)
                                              .addAddressesByChannelType(NotificationChannelType.EMAIL, toAddresses)
                                              .build();

    when(notificationSetupService.readNotificationGroup(APP_ID, NOTIFICATION_GROUP_ID)).thenReturn(notificationGroup);
    NotificationRule notificationRule = aNotificationRule()
                                            .withUuid(NOTIFICATION_RULE_ID)
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withBatchNotifications(true)
                                            .addNotificationGroup(notificationGroup)
                                            .build();

    InformationNotification notification =
        anInformationNotification()
            .withAccountId(ACCOUNT_ID)
            .withAppId(APP_ID)
            .withEntityId(WORKFLOW_EXECUTION_ID)
            .withEntityType(ORCHESTRATED_DEPLOYMENT)
            .withNotificationTemplateId(WORKFLOW_PHASE_SUCCESSFUL_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("WORKFLOW_NAME", WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "DATE", "DATE"))
            .build();

    when(wingsPersistence.upsert(any(Query.class), any(UpdateOperations.class)))
        .thenReturn(aNotificationBatch()
                        .withBatchId(NOTIFICATION_BATCH_ID)
                        .withNotificationRule(notificationRule)
                        .withNotifications(asList(notification))
                        .build());

    notificationDispatcherService.dispatchNotification(notification, asList(notificationRule));

    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("batchId");
    verify(end).equal("NOTIFICATION_RULE_ID-WORKFLOW_EXECUTION_ID");
    verify(updateOperations).set("batchId", "NOTIFICATION_RULE_ID-WORKFLOW_EXECUTION_ID");
    verify(updateOperations).set("notificationRule", notificationRule);
    verify(updateOperations).addToSet("notifications", notification);
    verify(wingsPersistence).upsert(any(Query.class), any(UpdateOperations.class));
    verify(wingsPersistence, never()).delete(any(NotificationBatch.class));
    verifyZeroInteractions(emailNotificationService);
  }

  @Test
  public void shouldSendBatchedNotificationsOnLastBatchNotification() {
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");

    NotificationGroup notificationGroup = aNotificationGroup()
                                              .withUuid(NOTIFICATION_GROUP_ID)
                                              .withAppId(APP_ID)
                                              .addAddressesByChannelType(NotificationChannelType.EMAIL, toAddresses)
                                              .build();

    when(notificationSetupService.readNotificationGroup(APP_ID, NOTIFICATION_GROUP_ID)).thenReturn(notificationGroup);
    NotificationRule notificationRule = aNotificationRule()
                                            .withUuid(NOTIFICATION_RULE_ID)
                                            .withExecutionScope(ExecutionScope.WORKFLOW)
                                            .withBatchNotifications(true)
                                            .addNotificationGroup(notificationGroup)
                                            .build();

    InformationNotification notification = anInformationNotification()
                                               .withAccountId(ACCOUNT_ID)
                                               .withAppId(APP_ID)
                                               .withEntityId(WORKFLOW_EXECUTION_ID)
                                               .withEntityType(ORCHESTRATED_DEPLOYMENT)
                                               .withNotificationTemplateId(WORKFLOW_FAILED_NOTIFICATION.name())
                                               .withNotificationTemplateVariables(ImmutableMap.of("WORKFLOW_NAME",
                                                   WORKFLOW_NAME, "ENV_NAME", ENV_NAME, "DATE", "DATE"))
                                               .build();

    when(wingsPersistence.upsert(any(Query.class), any(UpdateOperations.class)))
        .thenReturn(aNotificationBatch()
                        .withBatchId(NOTIFICATION_BATCH_ID)
                        .withNotificationRule(notificationRule)
                        .withNotifications(asList(notification))
                        .build());

    EmailTemplate emailTemplate = new EmailTemplate();
    emailTemplate.setBody(WORKFLOW_FAILED_NOTIFICATION.name());
    emailTemplate.setSubject(WORKFLOW_FAILED_NOTIFICATION.name());
    when(notificationMessageResolver.getEmailTemplate(WORKFLOW_FAILED_NOTIFICATION.name())).thenReturn(emailTemplate);

    notificationDispatcherService.dispatchNotification(notification, asList(notificationRule));

    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("batchId");
    verify(end).equal("NOTIFICATION_RULE_ID-WORKFLOW_EXECUTION_ID");
    verify(updateOperations).set("batchId", "NOTIFICATION_RULE_ID-WORKFLOW_EXECUTION_ID");
    verify(updateOperations).set("notificationRule", notificationRule);
    verify(updateOperations).addToSet("notifications", notification);
    verify(wingsPersistence).upsert(any(Query.class), any(UpdateOperations.class));
    verify(wingsPersistence).delete(any(NotificationBatch.class));
    verify(emailNotificationService)
        .sendAsync(EmailData.builder()
                       .to(toAddresses)
                       .cc(Collections.emptyList())
                       .subject(WORKFLOW_FAILED_NOTIFICATION.name())
                       .body(WORKFLOW_FAILED_NOTIFICATION.name())
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
               .sendMessage(slackConfig, channel, "harness", ENTITY_CREATE_NOTIFICATION.name()));
  }
}

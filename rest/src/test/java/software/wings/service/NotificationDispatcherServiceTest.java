package software.wings.service;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.EntityType;
import software.wings.beans.InformationNotification;
import software.wings.beans.InformationNotification.Builder;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationRule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
import software.wings.common.NotificationMessageResolver.NotificationMessageType;
import software.wings.common.UUIDGenerator;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SlackNotificationService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.WingsTestConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by rishi on 10/31/16.
 */
public class NotificationDispatcherServiceTest extends WingsBaseTest {
  @Inject @InjectMocks private NotificationDispatcherService notificationDispatcherService;

  @Mock private NotificationSetupService notificationSetupService;
  @Mock private EmailNotificationService<EmailData> emailNotificationService;
  @Mock private SlackNotificationService slackNotificationService;
  @Mock private SettingsService settingsService;

  @Test
  @Ignore
  public void shouldDispatchNotification() throws EmailException, TemplateException, IOException {
    String appId = UUIDGenerator.getUuid();
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");
    String uuid = UUIDGenerator.getUuid();
    //    List<NotificationRule> notificationRules = Lists.newArrayList(aNotificationRule().withAppId(appId)
    //        .addNotificationGroup(aNotificationGroup().withAppId(appId).addAddressesByChannelType(NotificationChannelType.EMAIL,
    //        toAddresses).build()).build());
    //    when(notificationSetupService.listNotificationRules(appId)).thenReturn(notificationRules);

    InformationNotification notification =
        anInformationNotification()
            .withUuid(uuid)
            .withAppId(appId)
            .withEnvironmentId(ENV_ID)
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .build();
    notificationDispatcherService.dispatchNotification(notification, new ArrayList<>());
    verify(emailNotificationService).sendAsync(toAddresses, null, notification.getUuid(), notification.getUuid());
  }

  @Test
  public void shouldDispatchSlackNotification() {
    InformationNotification notification =
        Builder.anInformationNotification()
            .withAccountId(ACCOUNT_ID)
            .withAppId(APP_ID)
            .withEntityId(ARTIFACT_ID)
            .withNotificationTemplateId(NotificationMessageType.ENTITY_CREATE_NOTIFICATION.name())
            .withNotificationTemplateVariables(
                ImmutableMap.of("ENTITY_NAME", "APP", "ENTITY_TYPE", EntityType.APPLICATION.name()))
            .build();

    NotificationRule notificationRule =
        aNotificationRule()
            .withAppId(APP_ID)
            .addNotificationGroup(aNotificationGroup()
                                      .withUuid("NOTIFICATION_GROUP_ID")
                                      .withAppId(APP_ID)
                                      .addAddressesByChannelType(NotificationChannelType.SLACK, asList("#channel"))
                                      .build())
            .build();

    SlackConfig slackConfig = new SlackConfig();
    slackConfig.setOutgoingWebhookUrl(WingsTestConstants.PORTAL_URL);
    when(settingsService.getGlobalSettingAttributesByType(SettingVariableTypes.SLACK.name()))
        .thenReturn(asList(SettingAttribute.Builder.aSettingAttribute().withValue(slackConfig).build()));
    when(notificationSetupService.readNotificationGroup(APP_ID, "NOTIFICATION_GROUP_ID"))
        .thenReturn(aNotificationGroup()
                        .withUuid("NOTIFICATION_GROUP_ID")
                        .withAppId(APP_ID)
                        .addAddressesByChannelType(NotificationChannelType.SLACK, asList("#channel"))
                        .build());

    notificationDispatcherService.dispatchNotification(notification, Arrays.asList(notificationRule));
    verify(slackNotificationService).sendMessage(eq(slackConfig), eq("#channel"), anyString(), anyString());
  }
}

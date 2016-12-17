package software.wings.service;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.EmbeddedUser.Builder.anEmbeddedUser;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;

import com.google.common.collect.Lists;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ApprovalNotification;
import software.wings.beans.EntityType;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationRule;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SlackConfig;
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
  public void shouldDispatchNotification() throws EmailException, TemplateException, IOException {
    String appId = UUIDGenerator.getUuid();
    List<String> toAddresses = Lists.newArrayList("a@b.com, c@d.com");
    String uuid = UUIDGenerator.getUuid();
    List<NotificationRule> notificationRules = Lists.newArrayList(
        aNotificationRule()
            .withAppId(appId)
            .addNotificationGroups(aNotificationGroup()
                                       .withAppId(appId)
                                       .addAddressesByChannelType(NotificationChannelType.EMAIL, toAddresses)
                                       .build())
            .build());
    when(notificationSetupService.listNotificationRules(appId)).thenReturn(notificationRules);

    InformationNotification notification = anInformationNotification()
                                               .withUuid(uuid)
                                               .withAppId(appId)
                                               .withEnvironmentId(ENV_ID)
                                               .withDisplayText("TEXT")
                                               .build();
    notificationDispatcherService.dispatchNotification(notification);
    verify(emailNotificationService).sendAsync(toAddresses, null, notification.getUuid(), notification.getUuid());
  }

  @Test
  public void shouldDispatchSlackNotification() {
    ApprovalNotification approvalNotification = anApprovalNotification()
                                                    .withAppId(APP_ID)
                                                    .withEntityId(ARTIFACT_ID)
                                                    .withEntityName(ARTIFACT_NAME)
                                                    .withEntityType(EntityType.ARTIFACT)
                                                    .withLastUpdatedBy(anEmbeddedUser().withName(USER_NAME).build())
                                                    .build();

    when(notificationSetupService.listNotificationRules(APP_ID))
        .thenReturn(asList(
            aNotificationRule()
                .withAppId(APP_ID)
                .addNotificationGroups(aNotificationGroup()
                                           .withAppId(APP_ID)
                                           .addAddressesByChannelType(NotificationChannelType.SLACK, asList("#channel"))
                                           .build())
                .build()));

    SlackConfig slackConfig = new SlackConfig();
    slackConfig.setOutgoingWebhookUrl(WingsTestConstants.PORTAL_URL);
    when(settingsService.getGlobalSettingAttributesByType(SettingVariableTypes.SLACK.name()))
        .thenReturn(asList(SettingAttribute.Builder.aSettingAttribute().withValue(slackConfig).build()));

    notificationDispatcherService.dispatchNotification(approvalNotification);
    verify(slackNotificationService).sendMessage(eq(slackConfig), eq("#channel"), anyString(), anyString());
  }
}

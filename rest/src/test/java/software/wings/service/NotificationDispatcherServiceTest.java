package software.wings.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.Lists;

import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.InformationNotification;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationRule;
import software.wings.common.UUIDGenerator;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.NotificationDispatcherService;
import software.wings.service.intfc.NotificationSetupService;

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
}

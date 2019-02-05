package software.wings.service.impl.notifications;

import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.common.NotificationMessageResolver;
import software.wings.common.NotificationMessageResolver.ChannelTemplate.EmailTemplate;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmailDispatcherTest extends WingsBaseTest {
  @Inject @InjectMocks EmailDispatcher emailDispatcher;
  @Mock private EmailNotificationService emailNotificationService;
  @Mock private NotificationMessageResolver notificationMessageResolver;

  @Test
  public void testEmailIsSent() {
    List<String> toAddresses = Arrays.asList("a@b.com, c@d.com");

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

    List<Notification> notifications = Collections.singletonList(notification);

    emailDispatcher.dispatch(notifications, toAddresses);
    Mockito.verify(emailNotificationService).sendAsync(Mockito.any(EmailData.class));
  }
}

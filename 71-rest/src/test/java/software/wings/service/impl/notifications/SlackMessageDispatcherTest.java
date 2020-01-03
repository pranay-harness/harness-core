package software.wings.service.impl.notifications;

import static io.harness.rule.OwnerRule.UJJAWAL;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.ORCHESTRATED_DEPLOYMENT;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.common.NotificationMessageResolver.NotificationMessageType.ENTITY_CREATE_NOTIFICATION;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.InformationNotification;
import software.wings.beans.Notification;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.common.NotificationMessageResolver;
import software.wings.service.intfc.SlackNotificationService;

import java.util.Collections;
import java.util.List;

public class SlackMessageDispatcherTest extends WingsBaseTest {
  @Inject @InjectMocks private SlackMessageDispatcher slackMessageDispatcher;
  @Mock private SlackNotificationService slackNotificationService;
  @Mock private NotificationMessageResolver notificationMessageResolver;

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldSendSlackMessage() {
    when(notificationMessageResolver.getSlackTemplate(ENTITY_CREATE_NOTIFICATION.name())).thenReturn("some template");

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

    SlackNotificationSetting setting = SlackNotificationSetting.emptyConfig();

    slackMessageDispatcher.dispatch(notifications, setting);
    Mockito.verify(slackNotificationService)
        .sendMessage(Mockito.eq(setting), Mockito.anyString(), Mockito.eq(HARNESS_NAME), Mockito.anyString(),
            Mockito.anyString());
  }
}

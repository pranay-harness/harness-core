package software.wings.service.impl;

import static io.harness.rule.OwnerRule.AMAN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SlackMessage;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SlackMessageSender;
import software.wings.service.intfc.SlackNotificationService;

/**
 * Created by anubhaw on 12/16/16.
 */

public class SlackNotificationServiceTest extends WingsBaseTest {
  @Inject private NotificationSetupService notificationSetupService;

  @Inject private AppService appService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SlackMessageSender slackMessageSender;
  @Inject @InjectMocks private SlackNotificationService slackNotificationService;

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void shouldSendMessageFromDelegate() {
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(true);
    when(delegateProxyFactory.get(any(Class.class), any(SyncTaskContext.class))).thenReturn(slackMessageSender);
    doNothing().when(slackMessageSender).send(any(SlackMessage.class), anyBoolean());

    slackNotificationService.sendMessage(
        new SlackNotificationSetting("name", "url"), "abc", "sender", "message", "accountId");
    verify(delegateProxyFactory, times(1)).get(any(), any());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void shouldSendMessageFromManager() {
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    when(delegateProxyFactory.get(any(Class.class), any(SyncTaskContext.class))).thenReturn(slackMessageSender);
    doNothing().when(slackMessageSender).send(any(SlackMessage.class), anyBoolean());

    slackNotificationService.sendMessage(
        new SlackNotificationSetting("name", "url"), "abc", "sender", "message", "accountId");
    verify(slackMessageSender, times(1)).send(any(SlackMessage.class), anyBoolean());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void shouldNotSendMessageIfWebhookUrlIsEmpty() {
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    when(delegateProxyFactory.get(any(Class.class), any(SyncTaskContext.class))).thenReturn(slackMessageSender);
    doNothing().when(slackMessageSender).send(any(SlackMessage.class), anyBoolean());

    slackNotificationService.sendMessage(
        new SlackNotificationSetting("name", ""), "abc", "sender", "message", "accountId");
    verify(slackMessageSender, times(0)).send(any(SlackMessage.class), anyBoolean());
    verify(slackMessageSender, times(0)).send(any(SlackMessage.class), anyBoolean());
    verify(delegateProxyFactory, times(0)).get(any(), any());
  }
}

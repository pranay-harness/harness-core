package software.wings.service.impl;

import static io.harness.rule.OwnerRule.AMAN;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SlackMessage;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SlackMessageSender;
import software.wings.service.intfc.SlackNotificationService;

import java.util.List;

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
  @Owner(emails = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldAddNotificationRulesForAllApplication() {
    List<Application> applications = appService.list(new PageRequest<>()).getResponse();
    applications.forEach(application -> {
      NotificationGroup notificationGroup =
          aNotificationGroup()
              .withName("Bots")
              .withAppId(application.getAppId())
              .addAddressesByChannelType(NotificationChannelType.SLACK, Lists.newArrayList("#wingsbot"))
              .addAddressesByChannelType(NotificationChannelType.EMAIL, Lists.newArrayList("wingsbot@wings.software"))
              .build();

      NotificationGroup savedNotificationGrp = notificationSetupService.createNotificationGroup(notificationGroup);

      NotificationRule notificationRule = aNotificationRule().addNotificationGroup(savedNotificationGrp).build();
    });
  }

  @Test
  @Owner(emails = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldSendMessageFromDelegate() {
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(true);
    when(delegateProxyFactory.get(any(Class.class), any(SyncTaskContext.class))).thenReturn(slackMessageSender);
    doNothing().when(slackMessageSender).send(any(SlackMessage.class));

    slackNotificationService.sendMessage(
        new SlackNotificationSetting("name", "url"), "abc", "sender", "message", "accountId");
    verify(delegateProxyFactory, times(1)).get(any(), any());
  }

  @Test
  @Owner(emails = AMAN)
  @Category(UnitTests.class)
  public void shouldSendMessageFromManager() {
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    when(delegateProxyFactory.get(any(Class.class), any(SyncTaskContext.class))).thenReturn(slackMessageSender);
    doNothing().when(slackMessageSender).send(any(SlackMessage.class));

    slackNotificationService.sendMessage(
        new SlackNotificationSetting("name", "url"), "abc", "sender", "message", "accountId");
    verify(slackMessageSender, times(1)).send(any(SlackMessage.class));
  }

  @Test
  @Owner(emails = AMAN)
  @Category(UnitTests.class)
  public void shouldNotSendMessageIfWebhookUrlIsEmpty() {
    when(featureFlagService.isEnabled(any(), anyString())).thenReturn(false);
    when(delegateProxyFactory.get(any(Class.class), any(SyncTaskContext.class))).thenReturn(slackMessageSender);
    doNothing().when(slackMessageSender).send(any(SlackMessage.class));

    slackNotificationService.sendMessage(
        new SlackNotificationSetting("name", ""), "abc", "sender", "message", "accountId");
    verify(slackMessageSender, times(0)).send(any(SlackMessage.class));
    verify(slackMessageSender, times(0)).send(any(SlackMessage.class));
    verify(delegateProxyFactory, times(0)).get(any(), any());
  }
}

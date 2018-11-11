package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static software.wings.beans.NotificationGroup.NotificationGroupBuilder.aNotificationGroup;
import static software.wings.beans.NotificationRule.NotificationRuleBuilder.aNotificationRule;
import static software.wings.service.impl.SlackNotificationServiceImpl.SLACK_WEBHOOK_URL_PREFIX;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import allbegray.slack.type.Payload;
import allbegray.slack.webhook.SlackWebhookClient;
import io.harness.beans.PageRequest;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationRule;
import software.wings.beans.SlackConfig;
import software.wings.service.impl.SlackNotificationServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.utils.WingsTestConstants;

import java.util.List;

/**
 * Created by anubhaw on 12/16/16.
 */

public class SlackNotificationServiceTest extends WingsBaseTest {
  public static final String CHANNEL = "#channel";
  public static final String SENDER = "sender";
  public static final String MESSAGE = "message";
  private SlackNotificationServiceImpl slackNotificationService = spy(new SlackNotificationServiceImpl());
  @Inject private NotificationSetupService notificationSetupService;

  @Mock private SlackWebhookClient slackWebhookClient;

  @Inject private AppService appService;

  @Test
  public void shouldSendMessage() {
    doReturn(slackWebhookClient).when(slackNotificationService).getWebhookClient(anyString());
    SlackConfig slackConfig = new SlackConfig();
    slackConfig.setOutgoingWebhookUrl(SLACK_WEBHOOK_URL_PREFIX + WingsTestConstants.PORTAL_URL);
    slackNotificationService.sendMessage(slackConfig, CHANNEL, SENDER, MESSAGE);
    ArgumentCaptor<Payload> argumentCaptor = ArgumentCaptor.forClass(Payload.class);
    verify(slackWebhookClient).post(argumentCaptor.capture());
    Payload payload = argumentCaptor.getValue();
    assertThat(payload.getChannel()).isEqualTo(CHANNEL);
    assertThat(payload.getUsername()).isEqualTo(SENDER);
    assertThat(payload.getText()).isEqualTo(MESSAGE);
  }

  @Test
  @Ignore
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
      //      NotificationRule savedNotificationRule =
      //      notificationSetupService.createNotificationRule(notificationRule);
    });
  }
}

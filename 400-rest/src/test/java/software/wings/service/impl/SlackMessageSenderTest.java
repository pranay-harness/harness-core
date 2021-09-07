package software.wings.service.impl;

import static io.harness.rule.OwnerRule.AMAN;
import static io.harness.rule.OwnerRule.MILOS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.SlackMessage;
import software.wings.service.impl.SlackMessageSenderImpl.SlackHttpClient;

import allbegray.slack.type.Payload;
import allbegray.slack.webhook.SlackWebhookClient;
import java.io.IOException;
import okhttp3.ResponseBody;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import retrofit2.Call;

@OwnedBy(HarnessTeam.CDC)
public class SlackMessageSenderTest extends WingsBaseTest {
  SlackMessageSenderImpl slackMessageSender = spy(new SlackMessageSenderImpl());
  @Mock private SlackWebhookClient slackWebhookClient;
  @Mock private SlackHttpClient slackHttpClient;
  @Mock private Call<ResponseBody> responseBodyCall;

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testMessageSending() throws IOException {
    SlackMessage slackMessage = new SlackMessage("https://hooks.slack.com/services/", "#channel", "sender",
        "message||something||something||completed||something");

    doReturn(slackWebhookClient).when(slackMessageSender).getWebhookClient(anyString());

    ArgumentCaptor<Payload> argumentCaptor = ArgumentCaptor.forClass(Payload.class);

    slackMessageSender.send(slackMessage, false, false);
    verify(slackWebhookClient).post(argumentCaptor.capture());

    Payload payload = argumentCaptor.getValue();
    assertThat(payload.getChannel()).isEqualTo("#channel");
    assertThat(payload.getUsername()).isEqualTo("sender");
    assertThat(payload.getText()).isEqualTo("message");

    doReturn(slackHttpClient).when(slackMessageSender).getSlackHttpClient(anyString(), anyBoolean());
    doReturn(responseBodyCall).when(slackHttpClient).PostMsg(any(), any());
    doReturn(null).when(responseBodyCall).execute();
    slackMessage = new SlackMessage("https://hooks.test.com/services/", "#channel", "sender",
        "message||something||something||completed||something");

    slackMessageSender.send(slackMessage, false, false);

    verify(slackHttpClient).PostMsg(anyString(), argumentCaptor.capture());
  }

  @Test
  @Owner(developers = MILOS)
  @Category(UnitTests.class)
  public void testMessageSendingWithCertValidation() throws IOException {
    SlackMessage slackMessage = new SlackMessage("https://hooks.slack.com/services/", "#channel", "sender",
        "message||something||something||completed||something");

    doReturn(slackWebhookClient).when(slackMessageSender).getWebhookClient(anyString());

    ArgumentCaptor<Payload> argumentCaptor = ArgumentCaptor.forClass(Payload.class);

    slackMessageSender.send(slackMessage, false, true);
    verify(slackWebhookClient).post(argumentCaptor.capture());

    Payload payload = argumentCaptor.getValue();
    assertThat(payload.getChannel()).isEqualTo("#channel");
    assertThat(payload.getUsername()).isEqualTo("sender");
    assertThat(payload.getText()).isEqualTo("message");

    doReturn(slackHttpClient).when(slackMessageSender).getSlackHttpClient(anyString(), anyBoolean());
    doReturn(responseBodyCall).when(slackHttpClient).PostMsg(any(), any());
    doReturn(null).when(responseBodyCall).execute();
    slackMessage = new SlackMessage("https://hooks.test.com/services/", "#channel", "sender",
        "message||something||something||completed||something");

    slackMessageSender.send(slackMessage, false, true);

    verify(slackHttpClient).PostMsg(anyString(), argumentCaptor.capture());
  }
}

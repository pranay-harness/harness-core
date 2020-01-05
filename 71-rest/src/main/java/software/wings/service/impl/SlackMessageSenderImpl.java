package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.common.NotificationConstants.ABORTED_COLOR;
import static software.wings.common.NotificationConstants.COMPLETED_COLOR;
import static software.wings.common.NotificationConstants.FAILED_COLOR;
import static software.wings.common.NotificationConstants.PAUSED_COLOR;
import static software.wings.common.NotificationConstants.RESUMED_COLOR;
import static software.wings.service.impl.SlackNotificationServiceImpl.SLACK_WEBHOOK_URL_PREFIX;

import com.google.common.collect.ImmutableList;

import allbegray.slack.SlackClientFactory;
import allbegray.slack.type.Attachment;
import allbegray.slack.type.Payload;
import allbegray.slack.webhook.SlackWebhookClient;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;
import software.wings.beans.SlackMessage;
import software.wings.service.intfc.SlackMessageSender;

import java.io.IOException;

@Slf4j
public class SlackMessageSenderImpl implements SlackMessageSender {
  private static final String WHITE_COLOR = "#FFFFFF";

  @Override
  public void send(SlackMessage slackMessage, boolean sendFromDelegate) {
    String outgoingWebhookUrl = slackMessage.getOutgoingWebhookUrl();
    String slackChannel = slackMessage.getSlackChannel();
    String senderName = slackMessage.getSenderName();
    String message = slackMessage.getMessage();

    logger.info("Slack sending message on channel: {}", slackChannel);

    Payload payload = new Payload();

    if (message.contains("||")) {
      Attachment attachment = new Attachment();
      String[] parts = message.split("\\|\\|");
      payload.setText(processText(parts[0]));
      attachment.setText(processText(parts[1]));
      attachment.setFooter(processText(parts[2]));
      attachment.setColor(getColor(parts[3]));
      attachment.setFooter_icon(format("https://s3.amazonaws.com/wings-assets/slackicons/%s.png", parts[3]));
      attachment.setMrkdwn_in(ImmutableList.of("text"));
      payload.setAttachments(ImmutableList.of(attachment));
    } else {
      payload.setText(processText(message));
    }

    if (isNotEmpty(slackChannel)) {
      if (slackChannel.charAt(0) != '#') {
        slackChannel = "#" + slackChannel;
      }
      payload.setChannel(slackChannel);
    }
    payload.setUsername(senderName);
    payload.setIcon_url("https://s3.amazonaws.com/wings-assets/slackicons/logo-slack.png");

    outgoingWebhookUrl = StringUtils.trim(outgoingWebhookUrl);
    // the SlackWebhookClient doesn't uses proxy. If you want to use the proxy on delegate, set
    // sendFromDelegate true.
    if (isSlackWebhookUrl(outgoingWebhookUrl) && !sendFromDelegate) {
      SlackWebhookClient webhookClient = getWebhookClient(outgoingWebhookUrl);
      webhookClient.post(payload);
    } else {
      sendGenericHttpPostRequest(outgoingWebhookUrl, payload);
    }
  }

  SlackWebhookClient getWebhookClient(final String webhookUrl) {
    return SlackClientFactory.createWebhookClient(webhookUrl);
  }

  private void sendGenericHttpPostRequest(String webhookUrl, Payload payload) {
    if (webhookUrl.endsWith("/")) {
      webhookUrl = webhookUrl.substring(0, webhookUrl.length() - 1);
    }
    int lastIndexOf = webhookUrl.lastIndexOf('/') + 1;
    String baseUrl = webhookUrl.substring(0, lastIndexOf);
    String webhookToken = webhookUrl.substring(lastIndexOf);
    try {
      getSlackHttpClient(baseUrl).PostMsg(webhookToken, payload).execute();
    } catch (IOException e) {
      throw new InvalidRequestException("Post message failed", e);
    }
  }

  private boolean isSlackWebhookUrl(final String webhookUrl) {
    return webhookUrl.startsWith(SLACK_WEBHOOK_URL_PREFIX);
  }

  SlackHttpClient getSlackHttpClient(String baseUrl) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(Http.getUnsafeOkHttpClient(baseUrl))
                                  .build();
    return retrofit.create(SlackHttpClient.class);
  }

  public interface SlackHttpClient { @POST Call<ResponseBody> PostMsg(@Url String url, @Body Payload payload); }

  private String processText(String message) {
    return message.replaceAll("<<<", "*<")
        .replaceAll("\\|-\\|", "|")
        .replaceAll(">>>", ">*")
        .replaceAll("\\\\n", "\n")
        .replaceAll("\\\\\\*", "*");
  }

  private String getColor(String status) {
    switch (status) {
      case "completed":
        return COMPLETED_COLOR;
      case "expired":
      case "rejected":
      case "failed":
        return FAILED_COLOR;
      case "paused":
        return PAUSED_COLOR;
      case "resumed":
        return RESUMED_COLOR;
      case "aborted":
        return ABORTED_COLOR;
      default:
        unhandled(status);
    }
    return WHITE_COLOR;
  }
}

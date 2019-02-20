package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static software.wings.common.Constants.ABORTED_COLOR;
import static software.wings.common.Constants.COMPLETED_COLOR;
import static software.wings.common.Constants.FAILED_COLOR;
import static software.wings.common.Constants.PAUSED_COLOR;
import static software.wings.common.Constants.RESUMED_COLOR;
import static software.wings.common.Constants.WHITE_COLOR;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;

import allbegray.slack.SlackClientFactory;
import allbegray.slack.type.Attachment;
import allbegray.slack.type.Payload;
import allbegray.slack.webhook.SlackWebhookClient;
import io.harness.exception.InvalidRequestException;
import io.harness.network.Http;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Url;
import software.wings.beans.notification.SlackNotificationConfiguration;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.service.intfc.SlackNotificationService;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by anubhaw on 12/14/16.
 */

@Singleton
public class SlackNotificationServiceImpl implements SlackNotificationService {
  private static final Logger log = LoggerFactory.getLogger(SlackNotificationServiceImpl.class);

  public static final String SLACK_WEBHOOK_URL_PREFIX = "https://hooks.slack.com/services/";

  @Override
  public void sendMessage(
      SlackNotificationConfiguration slackConfig, String slackChannel, String senderName, String message) {
    if (Objects.requireNonNull(slackConfig, "slack Config can't be null")
            .equals(SlackNotificationSetting.emptyConfig())) {
      return;
    }

    String webhookUrl = slackConfig.getOutgoingWebhookUrl();
    if (StringUtils.isEmpty(webhookUrl)) {
      log.error("Webhook URL is empty. No message will be sent. Config: {}, Message: {}", slackConfig, message);
      return;
    }

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

    webhookUrl = webhookUrl.trim();

    if (isSlackWebhookUrl(webhookUrl)) {
      SlackWebhookClient webhookClient = getWebhookClient(webhookUrl);
      webhookClient.post(payload);
    } else {
      sendGenericHttpPostRequest(webhookUrl, payload);
    }
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

  private boolean isSlackWebhookUrl(String webhookUrl) {
    return webhookUrl.startsWith(SLACK_WEBHOOK_URL_PREFIX);
  }

  public SlackWebhookClient getWebhookClient(String webhookUrl) {
    return SlackClientFactory.createWebhookClient(webhookUrl);
  }

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

  private SlackHttpClient getSlackHttpClient(String baseUrl) {
    final Retrofit retrofit = new Retrofit.Builder()
                                  .baseUrl(baseUrl)
                                  .addConverterFactory(JacksonConverterFactory.create())
                                  .client(Http.getUnsafeOkHttpClient(baseUrl))
                                  .build();
    return retrofit.create(SlackHttpClient.class);
  }

  public interface SlackHttpClient { @POST Call<ResponseBody> PostMsg(@Url String url, @Body Payload payload); }
}

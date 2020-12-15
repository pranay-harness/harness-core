package io.harness.notification.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

@Slf4j
public class SlackSenderImpl {
  public static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");
  private final OkHttpClient client = new OkHttpClient();

  public boolean send(List<String> slackWebhookUrls, String message, String notificationId) {
    boolean sent = false;
    for (String webhookUrl : slackWebhookUrls) {
      boolean ret = sendJSONMessage(message, webhookUrl);
      sent = sent || ret;
    }
    log.info(sent ? "Notificaition request {} sent" : "Failed to send notification for request {}", notificationId);
    return sent;
  }

  private boolean sendJSONMessage(String message, String slackWebhook) {
    try {
      RequestBody body = RequestBody.create(APPLICATION_JSON, message);
      Request request = new Request.Builder()
                            .url(slackWebhook)
                            .post(body)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "*/*")
                            .addHeader("Cache-Control", "no-cache")
                            .addHeader("Host", "hooks.slack.com")
                            .addHeader("accept-encoding", "gzip, deflate")
                            .addHeader("content-length", "798")
                            .addHeader("Connection", "keep-alive")
                            .addHeader("cache-control", "no-cache")
                            .build();

      try (Response response = client.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String bodyString = (null != response.body()) ? response.body().string() : "null";
          log.error("Response not Successful. Response body: {}", bodyString);
          return false;
        }
        return true;
      }
    } catch (Exception e) {
      log.error("Error sending post data", e);
    }
    return false;
  }
}

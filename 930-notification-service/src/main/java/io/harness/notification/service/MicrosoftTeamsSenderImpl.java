package io.harness.notification.service;

import static org.apache.http.entity.mime.MIME.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

@Slf4j
public class MicrosoftTeamsSenderImpl {
  private final OkHttpClient okHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();

  private static final MediaType APPLICATION_JSON = MediaType.parse(APPLICATION_JSON_UTF8_VALUE);

  public int sendMessage(String message, String webhookUrl) {
    try {
      RequestBody body = RequestBody.create(APPLICATION_JSON, message);
      Request request =
          new Request.Builder().url(webhookUrl).post(body).addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE).build();
      try (Response response = okHttpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String bodyString = (null != response.body()) ? response.body().string() : "null";
          log.error("Response not Successful. Response body: {}", bodyString);
        }
        return response.code();
      }
    } catch (Exception e) {
      log.error("Exception occurred at sendMessage(). Returning 400", e);
      return 400;
    }
  }

  public boolean send(List<String> microsoftTeamsWebhookUrls, String message, String notificationId) {
    boolean sent = false;
    for (String microsoftTeamsWebhookUrl : microsoftTeamsWebhookUrls) {
      int responseCode = sendMessage(message, microsoftTeamsWebhookUrl);
      sent = sent || (responseCode >= 200 && responseCode < 300);
    }
    log.info(sent ? "Notificaition request {} sent" : "Failed to send notification for request {}", notificationId);
    return sent;
  }
}

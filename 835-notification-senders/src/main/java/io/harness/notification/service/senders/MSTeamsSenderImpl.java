/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.notification.service.senders;

import static org.apache.http.entity.mime.MIME.CONTENT_TYPE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import io.harness.notification.beans.NotificationProcessingResponse;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class MSTeamsSenderImpl {
  private final OkHttpClient okHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();

  private static final MediaType APPLICATION_JSON = MediaType.parse(APPLICATION_JSON_VALUE);

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

  public NotificationProcessingResponse send(
      List<String> microsoftTeamsWebhookUrls, String message, String notificationId) {
    List<Boolean> results = new ArrayList<>();
    for (String microsoftTeamsWebhookUrl : microsoftTeamsWebhookUrls) {
      int responseCode = sendMessage(message, microsoftTeamsWebhookUrl);
      results.add(responseCode >= 200 && responseCode < 300);
    }
    return NotificationProcessingResponse.builder().result(results).build();
  }
}

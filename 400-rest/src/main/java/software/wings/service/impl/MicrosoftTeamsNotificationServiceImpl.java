/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static org.apache.http.entity.mime.MIME.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import software.wings.service.intfc.MicrosoftTeamsNotificationService;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class MicrosoftTeamsNotificationServiceImpl implements MicrosoftTeamsNotificationService {
  private final OkHttpClient okHttpClient = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();
  private static final MediaType APPLICATION_JSON = MediaType.parse(APPLICATION_JSON_UTF8_VALUE);

  @Override
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
}

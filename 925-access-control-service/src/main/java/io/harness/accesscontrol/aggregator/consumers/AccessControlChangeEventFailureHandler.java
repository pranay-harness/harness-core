/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.aggregator.consumers;

import io.harness.accesscontrol.commons.notifications.NotificationConfig;
import io.harness.aggregator.consumers.ChangeEventFailureHandler;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.debezium.engine.ChangeEvent;
import java.io.IOException;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.text.StrSubstitutor;
import org.apache.commons.text.StringEscapeUtils;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PL)
public class AccessControlChangeEventFailureHandler implements ChangeEventFailureHandler {
  private final NotificationConfig notificationConfig;
  private final String messageTemplate;
  private final OkHttpClient client;
  private static final String TEMPLATE_FILE_PATH = "io/harness/notification_templates/aggregator_failure.txt";

  @Inject
  public AccessControlChangeEventFailureHandler(NotificationConfig notificationConfig) {
    this.notificationConfig = notificationConfig;
    byte[] bytes;
    try {
      URL url = getClass().getClassLoader().getResource(TEMPLATE_FILE_PATH);
      if (url == null) {
        throw new UnexpectedException("Could not find the file path for notification template");
      }
      bytes = Resources.toByteArray(url);
    } catch (IOException e) {
      throw new UnexpectedException("Could not find notification template");
    }
    this.client = new OkHttpClient();
    this.messageTemplate = new String(bytes);
  }

  @Override
  public void handle(ChangeEvent<String, String> changeEvent, Throwable exception) {
    try {
      String message = String.format(
          "Access Control Aggregator failure: Environment: %s : Could not process change event with key %s and value %s. Exception %s",
          StringEscapeUtils.escapeJson(notificationConfig.getEnvironment()),
          StringEscapeUtils.escapeJson(changeEvent.key()), StringEscapeUtils.escapeJson(changeEvent.value()),
          StringEscapeUtils.escapeJson(exception.getMessage()));
      StrSubstitutor strSubstitutor = new StrSubstitutor(ImmutableMap.of("message", message));
      String finalMessage = strSubstitutor.replace(messageTemplate);
      sendJSONMessage(finalMessage, notificationConfig.getSlackWebhookUrl());
    } catch (Exception e) {
      log.error("Could not send failure event to slack due to error", e);
    }
  }

  private void sendJSONMessage(String message, String slackWebhook) throws IOException {
    MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");
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
      }
    }
  }
}

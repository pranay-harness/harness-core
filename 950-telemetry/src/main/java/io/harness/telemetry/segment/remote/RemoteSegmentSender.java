/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.telemetry.segment.remote;

import static io.harness.telemetry.utils.TelemetryDataUtils.filterNonNullProperties;

import static java.lang.Boolean.FALSE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.SegmentEventType;
import io.harness.telemetry.TelemetryConfiguration;
import io.harness.telemetry.TelemetryOption;
import io.harness.telemetry.TelemetryReporter;
import io.harness.telemetry.remote.GroupPayloadDTO;
import io.harness.telemetry.remote.IdentifyPayloadDTO;
import io.harness.telemetry.remote.TelemetryDataDTO;
import io.harness.telemetry.remote.TrackPayloadDTO;
import io.harness.telemetry.utils.TelemetryDataUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
@Singleton
public class RemoteSegmentSender implements TelemetryReporter {
  private final RemoteSegmentClient remoteSegmentClient;
  private final TelemetryConfiguration telemetryConfiguration;
  private static final int MAX_RETRY_TIMES = 5;
  private static final long RETRY_DURATION = 20000;
  private ExecutorService executorService;

  @Inject
  public RemoteSegmentSender(RemoteSegmentClient remoteSegmentClient, TelemetryConfiguration telemetryConfiguration) {
    this.remoteSegmentClient = remoteSegmentClient;
    this.telemetryConfiguration = telemetryConfiguration;
    executorService = Executors.newFixedThreadPool(5);
  }

  @Override
  public void sendTrackEvent(String eventName, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, String category, TelemetryOption... telemetryOption) {
    sendTrackEvent(eventName, null, null, properties, destinations, category, telemetryOption);
  }

  @Override
  public void sendTrackEvent(String eventName, String identity, String accountId, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, String category, TelemetryOption... telemetryOption) {
    // TODO: control by telemetry flag in account
    if (!telemetryConfiguration.isEnabled()) {
      return;
    }

    if (!isAllowedToSend(telemetryOption)) {
      return;
    }

    if (identity == null) {
      identity = TelemetryDataUtils.readIdentityFromPrincipal();
    }
    if (accountId == null) {
      accountId = TelemetryDataUtils.readAccountIdFromPrincipal();
    }

    Map<String, String> nonNullProperties = filterNonNullProperties(properties);
    nonNullProperties.put("groupId", accountId);
    if (category == null) {
      category = Category.COMMUNITY;
    }
    nonNullProperties.put("category", category);
    TrackPayloadDTO trackPayloadDTO = TrackPayloadDTO.builder().eventName(eventName).userId(identity).build();

    TelemetryDataDTO data = TelemetryDataDTO.builder()
                                .eventType(SegmentEventType.TRACK)
                                .properties(nonNullProperties)
                                .trackPayload(trackPayloadDTO)
                                .build();

    executorService.submit(() -> sendWithRetry(data));
  }

  @Override
  public void sendIdentifyEvent(String identity, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, TelemetryOption... telemetryOption) {
    // TODO: control by telemetry flag in account
    if (!telemetryConfiguration.isEnabled()) {
      return;
    }

    if (!isAllowedToSend(telemetryOption)) {
      return;
    }

    if (identity == null) {
      identity = TelemetryDataUtils.readIdentityFromPrincipal();
    }
    IdentifyPayloadDTO identifyPayloadDTO = IdentifyPayloadDTO.builder().userId(identity).build();

    Map<String, String> nonNullProperties = filterNonNullProperties(properties);
    TelemetryDataDTO data = TelemetryDataDTO.builder()
                                .eventType(SegmentEventType.IDENTIFY)
                                .properties(nonNullProperties)
                                .identifyPayload(identifyPayloadDTO)
                                .build();

    executorService.submit(() -> sendWithRetry(data));
  }

  @Override
  public void sendGroupEvent(String accountId, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, TelemetryOption... telemetryOption) {
    sendGroupEvent(accountId, null, properties, destinations, telemetryOption);
  }

  @Override
  public void sendGroupEvent(String accountId, String identity, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, TelemetryOption... telemetryOption) {
    sendGroupEvent(accountId, identity, properties, destinations, null, telemetryOption);
  }

  @Override
  public void sendGroupEvent(String accountId, String identity, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, Date timestamp, TelemetryOption... telemetryOption) {
    // TODO: control by telemetry flag in account
    if (!telemetryConfiguration.isEnabled()) {
      return;
    }

    if (!isAllowedToSend(telemetryOption)) {
      return;
    }

    if (identity == null) {
      identity = TelemetryDataUtils.readIdentityFromPrincipal();
    }
    if (timestamp != null) {
      properties.put("timestamp", timestamp);
    }
    GroupPayloadDTO groupPayloadDTO = GroupPayloadDTO.builder().groupId(accountId).userId(identity).build();

    Map<String, String> nonNullProperties = filterNonNullProperties(properties);
    TelemetryDataDTO data = TelemetryDataDTO.builder()
                                .eventType(SegmentEventType.GROUP)
                                .properties(nonNullProperties)
                                .groupPayload(groupPayloadDTO)
                                .build();

    executorService.submit(() -> sendWithRetry(data));
  }

  @Override
  public void flush() {
    // Do nothing
  }

  private boolean sendWithRetry(TelemetryDataDTO dataDTO) {
    Retry retry = Retry.of("Group event sender", getRetryConfig());
    return Retry
        .decorateSupplier(retry,
            () -> {
              try {
                Response<TelemetryDataDTO> response = remoteSegmentClient.reportEvent(dataDTO).execute();
                return response.isSuccessful();
              } catch (IOException e) {
                log.error("Send Group Event in retry Failed", e);
                return false;
              }
            })
        .get();
  }

  private RetryConfig getRetryConfig() {
    return RetryConfig.custom()
        .maxAttempts(MAX_RETRY_TIMES)
        .waitDuration(Duration.ofMillis(RETRY_DURATION))
        .retryOnResult(FALSE::equals)
        .retryExceptions(Exception.class)
        .build();
  }

  private boolean isAllowedToSend(TelemetryOption... telemetryOptions) {
    if (telemetryOptions.length == 1 && telemetryOptions[0] != null) {
      return telemetryOptions[0].isSendForCommunity();
    } else {
      return false;
    }
  }
}

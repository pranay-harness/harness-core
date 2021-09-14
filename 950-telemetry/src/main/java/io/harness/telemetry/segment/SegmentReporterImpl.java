/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.telemetry.segment;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.UserPrincipal;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.segment.analytics.messages.GroupMessage;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.TrackMessage;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.GTM)
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class SegmentReporterImpl implements TelemetryReporter {
  private SegmentSender segmentSender;
  private static final String USER_ID_KEY = "userId";
  private static final String GROUP_ID_KEY = "groupId";
  private static final String CATEGORY_KEY = "category";

  @Override
  public void sendTrackEvent(
      String eventName, HashMap<String, Object> properties, Map<Destination, Boolean> destinations, String category) {
    if (!segmentSender.isEnabled()) {
      return;
    }
    String identity = readIdentityFromPrincipal();
    String accountId = readAccountIdFromPrincipal();
    sendTrackEvent(eventName, identity, accountId, properties, destinations, category);
  }

  @Override
  public void sendTrackEvent(String eventName, String identity, String accountId, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, String category) {
    if (!segmentSender.isEnabled()) {
      return;
    }
    if (identity == null) {
      identity = readIdentityFromPrincipal();
    }
    if (accountId == null) {
      accountId = readAccountIdFromPrincipal();
    }
    if (category == null) {
      category = Category.GLOBAL;
    }
    if (properties == null) {
      properties = new HashMap<>();
    }

    try {
      TrackMessage.Builder trackMessageBuilder = TrackMessage.builder(eventName).userId(identity);

      properties.put(USER_ID_KEY, identity);
      properties.put(GROUP_ID_KEY, accountId);
      properties.put(CATEGORY_KEY, category);
      trackMessageBuilder.properties(properties);

      if (destinations != null) {
        destinations.forEach((k, v) -> trackMessageBuilder.enableIntegration(k.getDestinationName(), v));
      }
      segmentSender.enqueue(trackMessageBuilder);
    } catch (Exception e) {
      // protection from invalid data set in Builder causing runtime exception
      log.error("Build Track Event Failed", e);
    }
  }

  @Override
  public void sendIdentifyEvent(
      String identity, HashMap<String, Object> properties, Map<Destination, Boolean> destinations) {
    if (!segmentSender.isEnabled()) {
      return;
    }
    try {
      IdentifyMessage.Builder identifyMessageBuilder = IdentifyMessage.builder().userId(identity);

      if (properties != null) {
        identifyMessageBuilder.traits(properties);
      }
      if (destinations != null) {
        destinations.forEach((k, v) -> identifyMessageBuilder.enableIntegration(k.getDestinationName(), v));
      }
      segmentSender.enqueue(identifyMessageBuilder);
    } catch (Exception e) {
      // protection from invalid data set in Builder causing runtime exception
      log.error("Build Identify Event Failed", e);
    }
  }

  @Override
  public void sendGroupEvent(
      String accountId, HashMap<String, Object> properties, Map<Destination, Boolean> destinations) {
    if (!segmentSender.isEnabled()) {
      return;
    }
    String identity = readIdentityFromPrincipal();
    sendGroupEvent(accountId, identity, properties, destinations);
  }

  @Override
  public void sendGroupEvent(
      String accountId, String identity, HashMap<String, Object> properties, Map<Destination, Boolean> destinations) {
    if (!segmentSender.isEnabled()) {
      return;
    }
    sendGroupEvent(accountId, identity, properties, destinations, null);
  }

  @Override
  public void sendGroupEvent(String accountId, String identity, HashMap<String, Object> properties,
      Map<Destination, Boolean> destinations, Date timestamp) {
    if (!segmentSender.isEnabled()) {
      return;
    }
    if (identity == null) {
      identity = readIdentityFromPrincipal();
    }
    try {
      GroupMessage.Builder groupMessageBuilder = GroupMessage.builder(accountId).userId(identity);

      if (properties != null) {
        groupMessageBuilder.traits(properties);
      }
      if (destinations != null) {
        destinations.forEach((k, v) -> groupMessageBuilder.enableIntegration(k.getDestinationName(), v));
      }
      if (timestamp != null) {
        groupMessageBuilder.timestamp(timestamp);
      }
      segmentSender.enqueue(groupMessageBuilder);
    } catch (Exception e) {
      // protection from invalid data set in Builder causing runtime exception
      log.error("Build Group Event Failed", e);
    }
  }

  private String readIdentityFromPrincipal() {
    Principal principal = SecurityContextBuilder.getPrincipal();
    String identity = null;
    if (principal != null) {
      switch (principal.getType()) {
        case USER:
          UserPrincipal userPrincipal = (UserPrincipal) principal;
          identity = userPrincipal.getEmail();
          break;
        case API_KEY:
        case SERVICE:
          identity = principal.getName();
          break;
        default:
          log.warn("Unknown principal type from SecurityContextBuilder when reading identity");
      }
    }
    if (isEmpty(identity)) {
      log.debug("Failed to read identity from principal, use system user instead");
      // TODO add "-{accountId}" after "system" when accountId is provided in principal
      identity = "system";
    }
    return identity;
  }

  private String readAccountIdFromPrincipal() {
    Principal principal = SecurityContextBuilder.getPrincipal();
    String accountId = "unknown accountId";
    if (principal != null) {
      switch (principal.getType()) {
        case USER:
          UserPrincipal userPrincipal = (UserPrincipal) principal;
          accountId = userPrincipal.getAccountId();
          break;
        case API_KEY:
        case SERVICE:
          // TODO: accountId should be provided in principal
          break;
        default:
          log.warn("Unknown principal type from SecurityContextBuilder when reading accountId");
      }
    }
    return accountId;
  }
}

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EventConfig;
import io.harness.beans.EventDetail;
import io.harness.beans.EventPayload;
import io.harness.beans.EventStatus;

import software.wings.service.intfc.ownership.OwnedByApplication;

@OwnedBy(CDC)
public interface EventService extends OwnedByApplication {
  void deliverEvent(String accountId, String appId, EventPayload payload);

  void deliverEvent(String accountId, String appId, EventConfig eventConfig, EventPayload payload);

  void deliverEvent(String accountId, String appId, String eventConfigId, EventPayload payload);

  void sendTestEvent(String accountId, String appId, String eventConfigId);

  void updateEventStatus(String eventId, EventStatus eventStatus, EventDetail detail);
}

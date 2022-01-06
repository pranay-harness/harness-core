/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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

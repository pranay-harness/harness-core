/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.accesscontrol.scopes.harness.events;

import static io.harness.accesscontrol.scopes.harness.events.ScopeEventConsumer.SCOPE_EVENT_ENTITY_TYPES;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.ENTITY_TYPE;

import io.harness.accesscontrol.commons.events.EventFilter;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.consumer.Message;

import java.util.Map;

@OwnedBy(HarnessTeam.PL)
public class ScopeEventFilter implements EventFilter {
  @Override
  public boolean filter(Message message) {
    Map<String, String> metadataMap = message.getMessage().getMetadataMap();
    if (metadataMap == null || !metadataMap.containsKey(ACTION) || !metadataMap.containsKey(ENTITY_TYPE)) {
      return false;
    }
    String entityType = metadataMap.get(ENTITY_TYPE);
    String action = metadataMap.get(ACTION);
    return entityType != null && SCOPE_EVENT_ENTITY_TYPES.contains(entityType) && DELETE_ACTION.equals(action);
  }
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.service.intf;

import io.harness.eventsframework.entity_crud.EntityChangeDTO;

public interface GCPEntityChangeEventService {
  boolean processGCPEntityCreateEvent(EntityChangeDTO entityChangeDTO);
}

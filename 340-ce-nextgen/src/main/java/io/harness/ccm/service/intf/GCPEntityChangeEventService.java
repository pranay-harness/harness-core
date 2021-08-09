package io.harness.ccm.service.intf;

import io.harness.eventsframework.entity_crud.EntityChangeDTO;

public interface GCPEntityChangeEventService {
  boolean processGCPEntityCreateEvent(EntityChangeDTO entityChangeDTO);
}

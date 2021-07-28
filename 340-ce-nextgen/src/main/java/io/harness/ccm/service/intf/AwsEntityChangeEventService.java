package io.harness.ccm.service.intf;

import io.harness.eventsframework.entity_crud.EntityChangeDTO;

public interface AwsEntityChangeEventService {
  boolean processAWSEntityChangeEvent(EntityChangeDTO entityChangeDTO, String action);
}

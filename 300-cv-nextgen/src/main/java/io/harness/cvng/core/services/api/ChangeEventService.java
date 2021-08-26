package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.change.event.ChangeEventDTO;
import io.harness.cvng.core.entities.changeSource.event.ChangeEvent;

public interface ChangeEventService extends DeleteEntityByHandler<ChangeEvent> {
  Boolean register(String accountId, ChangeEventDTO changeEventDTO);
}

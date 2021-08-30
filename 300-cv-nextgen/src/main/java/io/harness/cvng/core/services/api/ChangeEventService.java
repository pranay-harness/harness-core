package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.change.event.ChangeEventDTO;

public interface ChangeEventService {
  Boolean register(String accountId, ChangeEventDTO changeEventDTO);
}

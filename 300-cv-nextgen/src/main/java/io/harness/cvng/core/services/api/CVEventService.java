package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.CVConfig;

public interface CVEventService {
  void sendConnectorCreateEvent(CVConfig cvConfig);

  void sendConnectorDeleteEvent(CVConfig cvConfig);
}

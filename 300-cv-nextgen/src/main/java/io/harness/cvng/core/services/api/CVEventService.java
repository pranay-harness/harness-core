package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.CVConfig;

public interface CVEventService {
  void sendConnectorCreateEvent(CVConfig cvConfig);

  void sendServiceCreateEvent(CVConfig cvConfig);

  void sendConnectorDeleteEvent(CVConfig cvConfig);

  void sendServiceDeleteEvent(CVConfig cvConfig);

  void sendEnvironmentCreateEvent(CVConfig cvConfig);

  void sendEnvironmentDeleteEvent(CVConfig cvConfig);
}

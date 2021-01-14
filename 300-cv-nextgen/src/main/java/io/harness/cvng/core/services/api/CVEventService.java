package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.verificationjob.entities.VerificationJob;

public interface CVEventService {
  void sendConnectorCreateEvent(CVConfig cvConfig);

  void sendServiceCreateEvent(CVConfig cvConfig);

  void sendConnectorDeleteEvent(CVConfig cvConfig);

  void sendServiceDeleteEvent(CVConfig cvConfig);

  void sendEnvironmentCreateEvent(CVConfig cvConfig);

  void sendEnvironmentDeleteEvent(CVConfig cvConfig);

  void sendVerificationJobEnvironmentCreateEvent(VerificationJob verificationJob);

  void sendVerificationJobServiceCreateEvent(VerificationJob verificationJob);

  void sendVerificationJobEnvironmentDeleteEvent(VerificationJob verificationJob);

  void sendVerificationJobServiceDeleteEvent(VerificationJob verificationJob);
}

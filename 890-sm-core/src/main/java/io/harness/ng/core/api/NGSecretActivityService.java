package io.harness.ng.core.api;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.dto.secrets.SecretDTOV2;

@OwnedBy(PL)
public interface NGSecretActivityService {
  void create(String accountIdentifier, SecretDTOV2 secret, NGActivityType ngActivityType);
  void deleteAllActivities(String accountIdentifier, String secretFQN);
}

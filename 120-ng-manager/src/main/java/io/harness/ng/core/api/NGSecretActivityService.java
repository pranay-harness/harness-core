package io.harness.ng.core.api;

import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.ng.core.dto.secrets.SecretDTOV2;

public interface NGSecretActivityService {
  void create(String accountIdentifier, SecretDTOV2 secret, NGActivityType ngActivityType);
  void deleteAllActivities(String accountIdentifier, String secretFQN);
}

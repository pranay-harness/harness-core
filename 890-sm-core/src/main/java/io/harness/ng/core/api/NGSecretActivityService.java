/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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

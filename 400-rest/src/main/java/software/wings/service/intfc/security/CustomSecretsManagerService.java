/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(PL)
public interface CustomSecretsManagerService {
  CustomSecretsManagerConfig getSecretsManager(@NotEmpty String accountId, @NotEmpty String configId);

  void setAdditionalDetails(CustomSecretsManagerConfig customSecretsManagerConfig);

  boolean validateSecretsManager(@NotEmpty String accountId, CustomSecretsManagerConfig customSecretsManagerConfig);

  String saveSecretsManager(@NotEmpty String accountId, CustomSecretsManagerConfig customSecretsManagerConfig);

  String updateSecretsManager(@NotEmpty String accountId, CustomSecretsManagerConfig customSecretsManagerConfig);

  boolean deleteSecretsManager(@NotEmpty String accountId, @NotEmpty String configId);
}

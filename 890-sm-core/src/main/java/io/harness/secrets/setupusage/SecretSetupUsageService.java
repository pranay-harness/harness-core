/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secrets.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import java.util.Set;

@OwnedBy(PL)
public interface SecretSetupUsageService {
  Set<SecretSetupUsage> getSecretUsage(String accountId, String secretTextId);
  Map<String, Set<String>> getUsagesAppEnvMap(String accountId, String secretTextId);
}

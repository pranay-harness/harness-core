/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.expression;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._940_SECRET_MANAGER_CLIENT)
public enum SecretManagerMode {
  APPLY,
  DRY_RUN,
  CHECK_FOR_SECRETS
}

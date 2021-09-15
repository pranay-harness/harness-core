/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.mutation.secretManager;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.secrets.QLUsageScope;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public abstract class QLSecretManagerInput {
  private boolean isDefault;
  private QLUsageScope usageScope;
}

/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secretManager;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.SecretManagerConfig;

import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager.QLSecretManagerBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class SecretManagerController {
  @Inject UsageScopeController usageScopeController;
  public QLSecretManagerBuilder populateSecretManager(
      SecretManagerConfig secretManager, QLSecretManagerBuilder builder) {
    return builder.id(secretManager.getUuid())
        .name(secretManager.getName())
        .usageScope(usageScopeController.populateUsageScope(secretManager.getUsageRestrictions()));
  }
}

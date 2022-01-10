/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secretManager;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.secretManager.QLCreateSecretManagerInput;
import software.wings.graphql.schema.mutation.secretManager.QLUpdateSecretManagerInput;
import software.wings.graphql.schema.type.secretManagers.QLSecretManager;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface SecretManagerMutationDataFetcher {
  QLSecretManager createSecretManager(QLCreateSecretManagerInput input, String accountId);

  QLSecretManager updateSecretManager(QLUpdateSecretManagerInput input, String accountId);

  void deleteSecretManager(String accountId, String secretManagerId);
}

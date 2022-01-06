/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secrets.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EncryptedDataParent;

import java.util.Map;
import java.util.Set;

@OwnedBy(PL)
public interface SecretSetupUsageBuilder {
  String ID_KEY = "_id";
  String ACCOUNT_ID_KEY = "accountId";

  Set<SecretSetupUsage> buildSecretSetupUsages(String accountId, String secretId,
      Map<String, Set<EncryptedDataParent>> parentsByParentIds, EncryptionDetail encryptionDetail);

  Map<String, Set<String>> buildAppEnvMap(
      String accountId, String secretId, Map<String, Set<EncryptedDataParent>> parentsByParentIds);
}

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

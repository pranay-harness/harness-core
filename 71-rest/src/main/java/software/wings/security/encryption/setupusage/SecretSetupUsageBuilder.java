package software.wings.security.encryption.setupusage;

import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;

import java.util.Map;
import java.util.Set;

public interface SecretSetupUsageBuilder {
  String ID_KEY = "_id";
  String ACCOUNT_ID_KEY = "accountId";

  Set<SecretSetupUsage> buildSecretSetupUsages(String accountId, String secretId,
      Map<String, Set<EncryptedDataParent>> parentsByParentIds, EncryptionDetail encryptionDetail);
}

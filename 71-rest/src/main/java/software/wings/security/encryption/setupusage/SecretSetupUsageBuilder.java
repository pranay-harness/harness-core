package software.wings.security.encryption.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import software.wings.security.encryption.EncryptedDataParent;
import software.wings.security.encryption.EncryptionDetail;

import java.util.Map;
import java.util.Set;

@OwnedBy(PL)
public interface SecretSetupUsageBuilder {
  String ID_KEY = "_id";
  String ACCOUNT_ID_KEY = "accountId";

  Set<SecretSetupUsage> buildSecretSetupUsages(String accountId, String secretId,
      Map<String, Set<EncryptedDataParent>> parentsByParentIds, EncryptionDetail encryptionDetail);
}

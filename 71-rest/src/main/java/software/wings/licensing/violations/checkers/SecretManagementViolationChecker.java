package software.wings.licensing.violations.checkers;

import com.google.inject.Inject;

import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import software.wings.beans.Account;
import software.wings.beans.FeatureEnabledViolation;
import software.wings.beans.FeatureViolation;
import software.wings.beans.KmsConfig;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.security.SecretManager;

import java.util.Collections;
import java.util.List;

public class SecretManagementViolationChecker implements FeatureViolationChecker {
  @Inject private SecretManager secretManager;

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    int customSecretManagersCount = getCustomSecretManagersCount(accountId);
    if (customSecretManagersCount != 0) {
      return Collections.singletonList(FeatureEnabledViolation.builder()
                                           .usageCount(customSecretManagersCount)
                                           .restrictedFeature(RestrictedFeature.SECRET_MANAGEMENT)
                                           .build());
    }

    return Collections.emptyList();
  }

  private int getCustomSecretManagersCount(String accountId) {
    List<EncryptionConfig> encryptionConfigs = secretManager.listEncryptionConfig(accountId);

    boolean isHarnessSecretManagerConfigured =
        encryptionConfigs.stream().anyMatch(SecretManagementViolationChecker::isHarnessSecretManager);

    return isHarnessSecretManagerConfigured ? encryptionConfigs.size() - 1 : encryptionConfigs.size();
  }

  private static boolean isHarnessSecretManager(EncryptionConfig secretManagerConfig) {
    if (secretManagerConfig.getEncryptionType() == EncryptionType.KMS) {
      KmsConfig kmsConfig = (KmsConfig) secretManagerConfig;
      return Account.GLOBAL_ACCOUNT_ID.equals(kmsConfig.getAccountId());
    }

    return false;
  }
}

package software.wings.security.encryption.setupusage;

import java.util.Set;

public interface SecretSetupUsageService {
  Set<SecretSetupUsage> getSecretUsage(String accountId, String secretTextId);
}

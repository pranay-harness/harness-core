package software.wings.security.encryption.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;

@OwnedBy(PL)
public interface SecretSetupUsageService {
  Set<SecretSetupUsage> getSecretUsage(String accountId, String secretTextId);
}

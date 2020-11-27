package io.harness.secrets.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import java.util.Set;

@OwnedBy(PL)
public interface SecretSetupUsageService {
  Set<SecretSetupUsage> getSecretUsage(String accountId, String secretTextId);
  Map<String, Set<String>> getUsagesAppEnvMap(String accountId, String secretTextId);
}

package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

@OwnedBy(PL)
public interface CustomSecretsManagerValidation {
  boolean isExecutableOnDelegate(CustomSecretsManagerConfig customSecretsManagerConfig);
}

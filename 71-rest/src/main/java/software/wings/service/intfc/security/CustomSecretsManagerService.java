package software.wings.service.intfc.security;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerConfig;

public interface CustomSecretsManagerService {
  CustomSecretsManagerConfig getSecretsManager(@NotEmpty String accountId, @NotEmpty String configId);

  boolean validateSecretsManager(@NotEmpty String accountId, CustomSecretsManagerConfig customSecretsManagerConfig);

  String saveSecretsManager(@NotEmpty String accountId, CustomSecretsManagerConfig customSecretsManagerConfig);

  String updateSecretsManager(@NotEmpty String accountId, CustomSecretsManagerConfig customSecretsManagerConfig);

  boolean deleteSecretsManager(@NotEmpty String accountId, @NotEmpty String configId);
}

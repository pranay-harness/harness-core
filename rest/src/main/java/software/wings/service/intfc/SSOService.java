package software.wings.service.intfc;

import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;

import java.io.InputStream;
import javax.validation.constraints.NotNull;

public interface SSOService {
  SSOConfig uploadSamlConfiguration(
      @NotNull String accountId, @NotNull InputStream inputStream, @NotNull String displayName);

  SSOConfig deleteSamlConfiguration(@NotNull String accountId);

  SSOConfig setAuthenticationMechanism(
      @NotNull String accountId, @NotNull AuthenticationMechanism authenticationMechanism);

  SSOConfig getAccountAccessManagementSettings(@NotNull String accountId);
}

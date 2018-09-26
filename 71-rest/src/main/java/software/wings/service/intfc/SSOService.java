package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapTestResponse;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.SSOConfig;

import java.io.InputStream;
import java.util.Collection;
import javax.validation.constraints.NotNull;

// TODO: Refactor this to make it more abstract and common across different SSO providers
public interface SSOService {
  SSOConfig uploadSamlConfiguration(
      @NotNull String accountId, @NotNull InputStream inputStream, @NotNull String displayName);

  SSOConfig deleteSamlConfiguration(@NotNull String accountId);

  SSOConfig setAuthenticationMechanism(
      @NotNull String accountId, @NotNull AuthenticationMechanism authenticationMechanism);

  SSOConfig getAccountAccessManagementSettings(@NotNull String accountId);

  LdapSettings createLdapSettings(@NotNull LdapSettings settings);

  LdapSettings updateLdapSettings(@NotNull LdapSettings settings);

  LdapSettings getLdapSettings(@NotBlank String accountId);

  LdapSettings deleteLdapSettings(@NotBlank String accountId);

  LdapTestResponse validateLdapConnectionSettings(@NotNull LdapSettings ldapSettings, @NotBlank String accountId);

  LdapTestResponse validateLdapUserSettings(@NotNull LdapSettings ldapSettings, @NotBlank String accountId);

  LdapTestResponse validateLdapGroupSettings(@NotNull LdapSettings ldapSettings, @NotBlank String accountId);

  LdapResponse validateLdapAuthentication(
      @NotNull LdapSettings ldapSettings, @NotBlank String identifier, @NotBlank String password);

  Collection<LdapGroupResponse> searchGroupsByName(@NotBlank String ldapSettingsId, @NotBlank String nameQuery);
}

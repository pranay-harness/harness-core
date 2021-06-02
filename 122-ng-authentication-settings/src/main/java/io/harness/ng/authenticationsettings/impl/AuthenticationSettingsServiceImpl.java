package io.harness.ng.authenticationsettings.impl;

import static io.harness.remote.client.RestClientUtils.getResponse;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.dtos.mechanisms.LDAPSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.NGAuthSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.OAuthSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.SAMLSettings;
import io.harness.ng.authenticationsettings.dtos.mechanisms.UsernamePasswordSettings;
import io.harness.ng.authenticationsettings.remote.AuthSettingsManagerClient;
import io.harness.ng.core.user.TwoFactorAdminOverrideSettings;

import software.wings.beans.loginSettings.LoginSettings;
import software.wings.beans.loginSettings.PasswordStrengthPolicy;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.OauthSettings;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.beans.sso.SamlSettings;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.LoginTypeResponse;
import software.wings.security.authentication.SSOConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsServiceImpl implements AuthenticationSettingsService {
  private final AuthSettingsManagerClient managerClient;

  @Override
  public AuthenticationSettingsResponse getAuthenticationSettings(String accountIdentifier) {
    Set<String> whitelistedDomains = getResponse(managerClient.getWhitelistedDomains(accountIdentifier));
    log.info("Whitelisted domains for accountId {}: {}", accountIdentifier, whitelistedDomains);
    SSOConfig ssoConfig = getResponse(managerClient.getAccountAccessManagementSettings(accountIdentifier));

    List<NGAuthSettings> settingsList = buildAuthSettingsList(ssoConfig, accountIdentifier);
    log.info("NGAuthSettings list for accountId {}: {}", accountIdentifier, settingsList);

    boolean twoFactorEnabled = getResponse(managerClient.twoFactorEnabled(accountIdentifier));

    return AuthenticationSettingsResponse.builder()
        .whitelistedDomains(whitelistedDomains)
        .ngAuthSettings(settingsList)
        .authenticationMechanism(ssoConfig.getAuthenticationMechanism())
        .twoFactorEnabled(twoFactorEnabled)
        .build();
  }

  @Override
  public void updateOauthProviders(String accountId, OAuthSettings oAuthSettings) {
    getResponse(managerClient.uploadOauthSettings(accountId,
        OauthSettings.builder()
            .allowedProviders(oAuthSettings.getAllowedProviders())
            .filter(oAuthSettings.getFilter())
            .accountId(accountId)
            .build()));
  }

  @Override
  public void updateAuthMechanism(String accountId, AuthenticationMechanism authenticationMechanism) {
    getResponse(managerClient.updateAuthMechanism(accountId, authenticationMechanism));
  }

  @Override
  public void removeOauthMechanism(String accountId) {
    getResponse(managerClient.deleteOauthSettings(accountId));
  }

  @Override
  public LoginSettings updateLoginSettings(
      String loginSettingsId, String accountIdentifier, LoginSettings loginSettings) {
    return getResponse(managerClient.updateLoginSettings(loginSettingsId, accountIdentifier, loginSettings));
  }

  @Override
  public void updateWhitelistedDomains(String accountIdentifier, Set<String> whitelistedDomains) {
    getResponse(managerClient.updateWhitelistedDomains(accountIdentifier, whitelistedDomains));
  }

  private List<NGAuthSettings> buildAuthSettingsList(SSOConfig ssoConfig, String accountIdentifier) {
    List<NGAuthSettings> settingsList = getNGAuthSettings(ssoConfig);

    LoginSettings loginSettings = getResponse(managerClient.getUserNamePasswordSettings(accountIdentifier));
    settingsList.add(UsernamePasswordSettings.builder().loginSettings(loginSettings).build());

    return settingsList;
  }

  private List<NGAuthSettings> getNGAuthSettings(SSOConfig ssoConfig) {
    List<SSOSettings> ssoSettings = ssoConfig.getSsoSettings();
    List<NGAuthSettings> result = new ArrayList<>();

    if (EmptyPredicate.isEmpty(ssoSettings)) {
      return result;
    }

    for (SSOSettings ssoSetting : ssoSettings) {
      if (ssoSetting.getType().equals(SSOType.SAML)) {
        SamlSettings samlSettings = (SamlSettings) ssoSetting;
        result.add(SAMLSettings.builder()
                       .identifier(samlSettings.getUuid())
                       .groupMembershipAttr(samlSettings.getGroupMembershipAttr())
                       .logoutUrl(samlSettings.getLogoutUrl())
                       .origin(samlSettings.getOrigin())
                       .displayName(samlSettings.getDisplayName())
                       .authorizationEnabled(samlSettings.isAuthorizationEnabled())
                       .build());

      } else if (ssoSetting.getType().equals(SSOType.OAUTH)) {
        OauthSettings oAuthSettings = (OauthSettings) ssoSetting;
        result.add(OAuthSettings.builder()
                       .allowedProviders(oAuthSettings.getAllowedProviders())
                       .filter(oAuthSettings.getFilter())
                       .build());
      } else if (ssoSetting.getType().equals(SSOType.LDAP)) {
        LdapSettings ldapSettings = (LdapSettings) ssoSetting;
        result.add(LDAPSettings.builder()
                       .identifier(ldapSettings.getUuid())
                       .connectionSettings(ldapSettings.getConnectionSettings())
                       .userSettingsList(ldapSettings.getUserSettingsList())
                       .groupSettingsList(ldapSettings.getGroupSettingsList())
                       .build());
      }
    }
    return result;
  }

  private RequestBody createPartFromString(String string) {
    if (string == null) {
      return null;
    }
    return RequestBody.create(MultipartBody.FORM, string);
  }

  @Override
  public SSOConfig uploadSAMLMetadata(@NotNull String accountId, @NotNull MultipartBody.Part inputStream,
      @NotNull String displayName, String groupMembershipAttr, @NotNull Boolean authorizationEnabled,
      String logoutUrl) {
    RequestBody displayNamePart = createPartFromString(displayName);
    RequestBody groupMembershipAttrPart = createPartFromString(groupMembershipAttr);
    RequestBody authorizationEnabledPart = createPartFromString(String.valueOf(authorizationEnabled));
    RequestBody logoutUrlPart = createPartFromString(logoutUrl);

    return getResponse(managerClient.uploadSAMLMetadata(
        accountId, inputStream, displayNamePart, groupMembershipAttrPart, authorizationEnabledPart, logoutUrlPart));
  }

  @Override
  public SSOConfig updateSAMLMetadata(@NotNull String accountId, MultipartBody.Part inputStream, String displayName,
      String groupMembershipAttr, @NotNull Boolean authorizationEnabled, String logoutUrl) {
    RequestBody displayNamePart = createPartFromString(displayName);
    RequestBody groupMembershipAttrPart = createPartFromString(groupMembershipAttr);
    RequestBody authorizationEnabledPart = createPartFromString(String.valueOf(authorizationEnabled));
    RequestBody logoutUrlPart = createPartFromString(logoutUrl);

    return getResponse(managerClient.updateSAMLMetadata(
        accountId, inputStream, displayNamePart, groupMembershipAttrPart, authorizationEnabledPart, logoutUrlPart));
  }

  @Override
  public SSOConfig deleteSAMLMetadata(@NotNull String accountIdentifier) {
    return getResponse(managerClient.deleteSAMLMetadata(accountIdentifier));
  }

  @Override
  public LoginTypeResponse getSAMLLoginTest(@NotNull String accountIdentifier) {
    return getResponse(managerClient.getSAMLLoginTest(accountIdentifier));
  }

  @Override
  public boolean setTwoFactorAuthAtAccountLevel(
      String accountIdentifier, TwoFactorAdminOverrideSettings twoFactorAdminOverrideSettings) {
    return getResponse(managerClient.setTwoFactorAuthAtAccountLevel(accountIdentifier, twoFactorAdminOverrideSettings));
  }

  @Override
  public PasswordStrengthPolicy getPasswordStrengthSettings(String accountIdentifier) {
    return getResponse(managerClient.getPasswordStrengthSettings(accountIdentifier));
  }
}

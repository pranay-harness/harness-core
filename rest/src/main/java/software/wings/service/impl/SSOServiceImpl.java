package software.wings.service.impl;

import com.google.inject.Inject;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import software.wings.beans.Account;
import software.wings.beans.ErrorCode;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SamlSettings;
import software.wings.exception.WingsException;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.AuthenticationUtil;
import software.wings.security.authentication.SSOConfig;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class SSOServiceImpl implements SSOService {
  @Inject AccountService accountService;
  @Inject SSOSettingService ssoSettingService;
  @Inject AuthenticationUtil authenticationUtil;
  @Inject SamlClientService samlClientService;

  @Override
  public SSOConfig uploadSamlConfiguration(String accountId, InputStream inputStream, String displayName) {
    try {
      String fileAsString = org.apache.commons.io.IOUtils.toString(inputStream, Charset.defaultCharset());
      buildAndUploadSamlSettings(accountId, fileAsString, displayName);
      return getAccountAccessManagementSettings(accountId);
    } catch (SamlException | IOException | URISyntaxException e) {
      throw new WingsException(ErrorCode.INVALID_SAML_CONFIGURATION, e);
    }
  }

  @Override
  public SSOConfig deleteSamlConfiguration(String accountId) {
    ssoSettingService.deleteSamlSettings(accountId);
    return setAuthenticationMechanism(accountId, AuthenticationMechanism.USER_PASSWORD);
  }

  @Override
  public SSOConfig setAuthenticationMechanism(String accountId, AuthenticationMechanism mechanism) {
    Account account = accountService.get(accountId);
    account.setAuthenticationMechanism(mechanism);
    accountService.update(account);
    return getAccountAccessManagementSettings(accountId);
  }

  @Override
  public SSOConfig getAccountAccessManagementSettings(String accountId) {
    Account account = accountService.get(accountId);
    return SSOConfig.builder()
        .accountId(accountId)
        .authenticationMechanism(account.getAuthenticationMechanism())
        .ssoSettings(getSSOSettings(account))
        .build();
  }

  private List<SSOSettings> getSSOSettings(Account account) {
    List<SSOSettings> settings = new ArrayList<>();
    SamlSettings samlSettings = ssoSettingService.getSamlSettingsByAccountId(account.getUuid());
    if (samlSettings != null) {
      settings.add(samlSettings.getPublicSSOSettings());
    }
    return settings;
  }

  private SamlSettings buildAndUploadSamlSettings(String accountId, String fileAsString, String displayName)
      throws SamlException, URISyntaxException {
    SamlClient samlClient = samlClientService.getSamlClient(fileAsString);
    SamlSettings samlSettings = SamlSettings.builder()
                                    .metaDataFile(fileAsString)
                                    .url(samlClient.getIdentityProviderUrl())
                                    .accountId(accountId)
                                    .displayName(displayName)
                                    .origin(new URI(samlClient.getIdentityProviderUrl()).getHost())
                                    .build();
    return ssoSettingService.saveSamlSettings(samlSettings);
  }
}

package software.wings.security.saml;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import software.wings.beans.Account;
import software.wings.beans.ErrorCode;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
import software.wings.exception.WingsException;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.AuthenticationUtil;
import software.wings.service.intfc.SSOSettingService;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

@Singleton
public class SamlClientService {
  @Inject AuthenticationUtil authenticationUtil;
  @Inject SSOSettingService ssoSettingService;

  public SamlClient getSamlClientFromAccount(Account account) throws SamlException {
    SamlSettings samlSettings = ssoSettingService.getSamlSettingsByAccountId(account.getUuid());
    return getSamlClient(samlSettings);
  }

  public SamlClient getSamlClientFromIdpUrl(String idpUrl) throws SamlException {
    SamlSettings samlSettings = ssoSettingService.getSamlSettingsByIdpUrl(idpUrl);
    return getSamlClient(samlSettings);
  }

  public SamlClient getSamlClient(SamlSettings samlSettings) throws SamlException {
    if (samlSettings == null) {
      throw new WingsException(ErrorCode.SAML_IDP_CONFIGURATION_NOT_AVAILABLE);
    }
    return getSamlClient(samlSettings.getMetaDataFile());
  }

  public SamlClient getSamlClient(String samlData) throws SamlException {
    return SamlClient.fromMetadata(
        "Harness", null, new InputStreamReader(new ByteArrayInputStream(samlData.getBytes())));
  }

  public SamlRequest generateSamlRequest(User user) {
    Account primaryAccount = authenticationUtil.getPrimaryAccount(user).get();
    if (primaryAccount.getAuthenticationMechanism().equals(AuthenticationMechanism.SAML)) {
      SamlRequest samlRequest = new SamlRequest();
      try {
        SamlClient samlClient = getSamlClientFromAccount(primaryAccount);
        samlRequest.setIdpRedirectUrl(samlClient.getIdentityProviderUrl());
        return samlRequest;

      } catch (SamlException e) {
        throw new WingsException(e);
      }
    }

    throw new WingsException(ErrorCode.INVALID_AUTHENTICATION_MECHANISM, WingsException.HARMLESS);
  }
}

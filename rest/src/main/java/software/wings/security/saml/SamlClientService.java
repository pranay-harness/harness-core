package software.wings.security.saml;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
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

  @SuppressFBWarnings({"DM_DEFAULT_ENCODING", "DM_DEFAULT_ENCODING"})
  public SamlClient getSamlClient(String samlData) throws SamlException {
    return SamlClient.fromMetadata(
        "Harness", null, new InputStreamReader(new ByteArrayInputStream(samlData.getBytes())));
  }

  public SamlRequest generateSamlRequest(User user) {
    Account primaryAccount = authenticationUtil.getPrimaryAccount(user);
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

    throw new WingsException(ErrorCode.INVALID_AUTHENTICATION_MECHANISM, USER);
  }

  public SamlClient getSamlClientFromOrigin(String origin) throws SamlException {
    SamlSettings samlSettings = ssoSettingService.getSamlSettingsByOrigin(origin);
    return getSamlClient(samlSettings);
  }
}

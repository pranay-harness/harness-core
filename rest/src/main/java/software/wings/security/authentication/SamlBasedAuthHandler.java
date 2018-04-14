package software.wings.security.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.coveo.saml.SamlResponse;
import software.wings.beans.ErrorCode;
import software.wings.beans.User;
import software.wings.exception.WingsException;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;

@Singleton
public class SamlBasedAuthHandler implements AuthHandler {
  @Inject private SamlClientService samlClientService;
  @Inject private AuthenticationUtil authenticationUtil;
  @Inject private AccountService accountService;

  @Override
  public User authenticate(String... credentials) {
    try {
      if (credentials == null || credentials.length != 2) {
        throw new WingsException("Invalid arguments while authenticating using SAML");
      }
      String idpUrl = credentials[0];
      String samlResponseString = credentials[1];
      SamlClient samlClient = samlClientService.getSamlClientFromIdpUrl(idpUrl);
      SamlResponse samlResponse = samlClient.decodeAndValidateSamlResponse(samlResponseString);
      String nameId = samlResponse.getNameID();
      User user = authenticationUtil.getUser(nameId);
      if (user == null) {
        throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
      }
      return user;
    } catch (SamlException e) {
      throw new WingsException("Saml Authentication Failed", e);
    }
  }

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.SAML;
  }
}

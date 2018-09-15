package software.wings.security.authentication;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.coveo.saml.SamlResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Optional;

@Singleton
public class SamlBasedAuthHandler implements AuthHandler {
  @Inject private SamlClientService samlClientService;
  @Inject private AuthenticationUtil authenticationUtil;
  @Inject private AccountService accountService;
  private static Logger logger = LoggerFactory.getLogger(SamlBasedAuthHandler.class);
  private static final String GOOGLE = "accounts.google.com";
  private static final String AZURE = "login.microsoftonline.com";

  @Override
  public User authenticate(String... credentials) {
    try {
      if (credentials == null || credentials.length != 2) {
        throw new WingsException("Invalid arguments while authenticating using SAML");
      }
      String idpUrl = credentials[0];
      String samlResponseString = credentials[1];
      return decodeResponse(idpUrl, samlResponseString);
    } catch (SamlException | URISyntaxException e) {
      throw new WingsException("Saml Authentication Failed", e);
    }
  }

  private User decodeResponse(String idpUrl, String samlResponseString) throws SamlException, URISyntaxException {
    String host = new URI(idpUrl).getHost();
    switch (host) {
      case GOOGLE: {
        Iterator<SamlSettings> samlSettingsIterator = samlClientService.getSamlSettingsFromOrigin(host);
        if (samlSettingsIterator != null) {
          while (samlSettingsIterator.hasNext()) {
            final SamlSettings samlSettings = samlSettingsIterator.next();
            try {
              Optional<NameValuePair> nameValuePair = URLEncodedUtils.parse(new URI(samlSettings.getUrl()), "UTF-8")
                                                          .stream()
                                                          .filter(pair -> pair.getName().equals("idpid"))
                                                          .findFirst();
              if (nameValuePair.isPresent() && idpUrl.contains(nameValuePair.get().getValue())) {
                return getUser(samlResponseString, samlSettings);
              }
            } catch (SamlException e) {
              logger.warn(
                  "Could not validate SAML Response idpUrl:[{}], samlSettings url:[{}]", idpUrl, samlSettings.getUrl());
            }
          }

          // if you have reached here, it means none of the SAML IDP metadata matched, try a brute force approach the
          // 2nd time
          User user = getUserForIdpUrl(idpUrl, samlResponseString);
          if (user != null) {
            return user;
          }
        }
      } break;
      case AZURE: {
        Iterator<SamlSettings> samlSettingsIterator = samlClientService.getSamlSettingsFromOrigin(host);
        if (samlSettingsIterator != null) {
          while (samlSettingsIterator.hasNext()) {
            SamlSettings samlSettings = samlSettingsIterator.next();
            if (idpUrl.startsWith(samlSettings.getUrl())) {
              try {
                return getUser(samlResponseString, samlSettings);
              } catch (SamlException e) {
                logger.warn("Could not validate SAML Response idpUrl:[{}], samlSettings url:[{}]", idpUrl,
                    samlSettings.getUrl());
              }
            }
          }
          // if you have reached here, it means none of the SAML IDP metadata matched, try a brute force approach the
          // 2nd time

          User user = getUserForIdpUrl(idpUrl, samlResponseString);
          if (user != null) {
            return user;
          }
        }
      }

      break;
      default: {
        User user = getUserForIdpUrl(idpUrl, samlResponseString);
        if (user != null) {
          return user;
        }
      }
    }

    logger.info("No IDP metadata could be found for URL [{}]", idpUrl);
    throw new WingsException("Saml Authentication Failed");
  }

  private User getUserForIdpUrl(String idpUrl, String samlResponseString) throws URISyntaxException {
    String host = new URI(idpUrl).getHost();
    Iterator<SamlSettings> samlSettingsIterator = samlClientService.getSamlSettingsFromOrigin(host);
    if (samlSettingsIterator != null) {
      while (samlSettingsIterator.hasNext()) {
        SamlSettings samlSettings = samlSettingsIterator.next();
        try {
          return getUser(samlResponseString, samlSettings);
        } catch (SamlException e) {
          logger.warn(
              "Could not validate SAML Response idpUrl:[{}], samlSettings url:[{}]", idpUrl, samlSettings.getUrl());
        }
      }
    }
    return null;
  }

  private User getUser(String samlResponseString, SamlSettings samlSettings) throws SamlException {
    SamlClient samlClient = samlClientService.getSamlClient(samlSettings);
    SamlResponse samlResponse = samlClient.decodeAndValidateSamlResponse(samlResponseString);
    String nameId = samlResponse.getNameID();
    User user = authenticationUtil.getUser(nameId);
    if (user == null) {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
    }
    validateUser(user, samlSettings.getAccountId());
    return user;
  }

  // TODO : revisit this method when we are doing SAML authorization
  protected void validateUser(User user, String accountId) {
    if (user.getAccounts().parallelStream().filter(account -> account.getUuid().equals(accountId)).count() == 0) {
      logger.warn("User : [{}] not part of accountId : [{}]", user.getEmail(), accountId);
      throw new WingsException(ErrorCode.ACCESS_DENIED);
    }
  }

  @Override
  public AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.SAML;
  }
}

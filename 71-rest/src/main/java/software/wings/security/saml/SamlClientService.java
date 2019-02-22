package software.wings.security.saml;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.exception.WingsException.USER;
import static org.opensaml.xml.util.Base64.encodeBytes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.http.client.utils.URIBuilder;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
import software.wings.security.authentication.AuthenticationMechanism;
import software.wings.security.authentication.AuthenticationUtil;
import software.wings.service.intfc.SSOSettingService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Iterator;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

@Singleton
public class SamlClientService {
  public static final String SAML_REQUEST_URI_KEY = "SAMLRequest";
  @Inject AuthenticationUtil authenticationUtil;
  @Inject SSOSettingService ssoSettingService;
  private static final String GOOGLE_HOST = "accounts.google.com";
  private static final String AZURE_HOST = "login.microsoftonline.com";

  /**
   * This field is identifier of SAML application and is used to point to the
   * specific application for Saml redirection in case of azure and gcp.
   */
  public static final String RELYING_PARTY_IDENTIFIER = "app.harness.io";

  public String getHost(@NotBlank String url) throws URISyntaxException {
    return new URI(url).getHost();
  }

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
    return SamlClient.fromMetadata(RELYING_PARTY_IDENTIFIER, null,
        new InputStreamReader(new ByteArrayInputStream(samlData.getBytes(UTF_8)), UTF_8));
  }

  public SSORequest generateSamlRequest(User user) {
    Account primaryAccount = authenticationUtil.getPrimaryAccount(user);
    if (primaryAccount.getAuthenticationMechanism().equals(AuthenticationMechanism.SAML)) {
      SSORequest SSORequest = new SSORequest();
      try {
        SamlClient samlClient = getSamlClientFromAccount(primaryAccount);
        URIBuilder redirectionUri = new URIBuilder(samlClient.getIdentityProviderUrl());
        redirectionUri.addParameter(SAML_REQUEST_URI_KEY, encodeParamaeters(samlClient.getSamlRequest()));
        SSORequest.setIdpRedirectUrl(redirectionUri.toString());
        return SSORequest;
      } catch (SamlException | URISyntaxException | IOException e) {
        throw new WingsException("Generating Saml request failed for user: [%s]", e);
      }
    }

    throw new WingsException(ErrorCode.INVALID_AUTHENTICATION_MECHANISM, USER);
  }

  public String encodeParamaeters(String encodedXmlParam) throws IOException {
    String decodedXmlParam = new String(Base64.getMimeDecoder().decode(encodedXmlParam), UTF_8);
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    Deflater deflater = new Deflater(8, true);
    DeflaterOutputStream deflaterStream = new DeflaterOutputStream(bytesOut, deflater);
    deflaterStream.write(decodedXmlParam.getBytes(UTF_8));
    deflaterStream.finish();
    return encodeBytes(bytesOut.toByteArray(), 8);
  }

  public SamlClient getSamlClientFromOrigin(String origin) throws SamlException {
    SamlSettings samlSettings = ssoSettingService.getSamlSettingsByOrigin(origin);
    return getSamlClient(samlSettings);
  }

  public Iterator<SamlSettings> getSamlSettingsFromOrigin(String origin) {
    return ssoSettingService.getSamlSettingsIteratorByOrigin(origin);
  }

  public HostType getHostType(@NotBlank String url) throws URISyntaxException {
    switch (getHost(url)) {
      case GOOGLE_HOST:
        return HostType.GOOGLE;
      case AZURE_HOST:
        return HostType.AZURE;
      default:
        return HostType.OTHER;
    }
  }

  public enum HostType { GOOGLE, AZURE, OTHER }
}

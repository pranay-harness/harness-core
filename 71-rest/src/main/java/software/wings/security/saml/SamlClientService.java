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
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.service.impl.AccountServiceImpl;
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
  @Inject AuthenticationUtils authenticationUtils;
  @Inject AccountServiceImpl accountServiceImpl;
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
    Account primaryAccount = authenticationUtils.getDefaultAccount(user);
    if (primaryAccount.getAuthenticationMechanism() == AuthenticationMechanism.SAML) {
      return generateSamlRequestFromAccount(primaryAccount);
    }

    throw new WingsException(ErrorCode.INVALID_AUTHENTICATION_MECHANISM, USER);
  }

  public SSORequest generateTestSamlRequest(String accountId) {
    Account account = accountServiceImpl.get(accountId);
    return generateSamlRequestFromAccount(account);
  }

  /**
   * To be used generateSamlRequest and generateSamlRequest for common functionality
   * @param account account passed from previous functions
   * @return SSORequest for redirection to SSO provider
   * @throws Exception error while creating request
   */
  private SSORequest generateSamlRequestFromAccount(Account account) {
    SSORequest ssoRequest = new SSORequest();
    try {
      SamlClient samlClient = getSamlClientFromAccount(account);
      URIBuilder redirectionUri = new URIBuilder(samlClient.getIdentityProviderUrl());
      redirectionUri.addParameter(SAML_REQUEST_URI_KEY, encodeParamaeters(samlClient.getSamlRequest()));
      ssoRequest.setIdpRedirectUrl(redirectionUri.toString());
      return ssoRequest;
    } catch (SamlException | URISyntaxException | IOException e) {
      String accountId = account.getUuid();
      throw new WingsException(String.format("Generating Saml request failed for account: [%s]", accountId), e);
    }
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

  // TODO: this method should return HIterator and close at the end
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

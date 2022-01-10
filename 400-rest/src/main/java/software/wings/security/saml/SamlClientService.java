/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.saml;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;

import static com.google.common.base.Charsets.UTF_8;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.ng.core.account.AuthenticationMechanism;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.service.impl.AccountServiceImpl;
import software.wings.service.intfc.SSOSettingService;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.hibernate.validator.constraints.NotBlank;
import org.jooq.tools.StringUtils;

@OwnedBy(PL)
@Singleton
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
@Slf4j
public class SamlClientService {
  public static final String SAML_REQUEST_URI_KEY = "SAMLRequest";
  public static final String SAML_TRIGGER_TYPE = "triggerType"; // login or test
  @Inject AuthenticationUtils authenticationUtils;
  @Inject AccountServiceImpl accountServiceImpl;
  @Inject SSOSettingService ssoSettingService;
  private static final String GOOGLE_HOST = "accounts.google.com";
  private static final String AZURE_HOST = "login.microsoftonline.com";
  private static final String ACCOUNT_ID = "accountId";

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
    String entityIdentifier = StringUtils.isEmpty(samlSettings.getEntityIdentifier())
        ? RELYING_PARTY_IDENTIFIER
        : samlSettings.getEntityIdentifier();
    log.info("SAMLClientService is using relayingPartyIdentifier : {} for accountId: {} and samlSettings : {}",
        entityIdentifier, samlSettings.getAccountId(), samlSettings);
    return SamlClient.fromMetadata(entityIdentifier, null,
        new InputStreamReader(new ByteArrayInputStream(samlSettings.getMetaDataFile().getBytes(UTF_8)), UTF_8));
  }

  public SamlClient getSamlClient(String entityIdentifier, String samlMetaData) throws SamlException {
    entityIdentifier = StringUtils.isEmpty(entityIdentifier) ? RELYING_PARTY_IDENTIFIER : entityIdentifier;
    return SamlClient.fromMetadata(
        entityIdentifier, null, new InputStreamReader(new ByteArrayInputStream(samlMetaData.getBytes(UTF_8)), UTF_8));
  }

  public SSORequest generateSamlRequest(User user) {
    Account primaryAccount = authenticationUtils.getDefaultAccount(user);
    if (primaryAccount.getAuthenticationMechanism() == AuthenticationMechanism.SAML) {
      return generateSamlRequestFromAccount(primaryAccount, false);
    }

    throw new WingsException(ErrorCode.INVALID_AUTHENTICATION_MECHANISM, USER);
  }

  public SSORequest generateTestSamlRequest(String accountId) {
    Account account = accountServiceImpl.get(accountId);
    return generateSamlRequestFromAccount(account, true);
  }

  /**
   * To be used generateSamlRequest and generateSamlRequest for common functionality
   * @param account account passed from previous functions
   * @return SSORequest for redirection to SSO provider
   * @throws Exception error while creating request
   */
  public SSORequest generateSamlRequestFromAccount(Account account, boolean isTestConnectionRequest) {
    SSORequest ssoRequest = new SSORequest();
    String triggerType = isTestConnectionRequest ? "test" : "login";
    try {
      SamlClient samlClient = getSamlClientFromAccount(account);
      URIBuilder redirectionUri = new URIBuilder(samlClient.getIdentityProviderUrl());
      redirectionUri.addParameter(SAML_REQUEST_URI_KEY, encodeParamaeters(samlClient.getSamlRequest()));
      redirectionUri.addParameter("RelayState", SAML_TRIGGER_TYPE + "=" + triggerType);
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
    return Base64.getEncoder().encodeToString(bytesOut.toByteArray());
  }

  // TODO: this method should return HIterator and close at the end
  public Iterator<SamlSettings> getSamlSettingsFromOrigin(String origin, String accountId) {
    return ssoSettingService.getSamlSettingsIteratorByOrigin(origin, accountId);
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

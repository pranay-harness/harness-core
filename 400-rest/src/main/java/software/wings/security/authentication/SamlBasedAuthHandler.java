/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.saml.SamlClientService.SAML_TRIGGER_TYPE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.account.AuthenticationMechanism;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.beans.sso.SamlSettings;
import software.wings.logcontext.UserLogContext;
import software.wings.security.saml.SamlClientService;
import software.wings.security.saml.SamlClientService.HostType;
import software.wings.security.saml.SamlUserGroupSync;
import software.wings.service.intfc.SSOSettingService;

import com.coveo.saml.SamlClient;
import com.coveo.saml.SamlException;
import com.coveo.saml.SamlResponse;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSAnyImpl;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;

@OwnedBy(PL)
@Singleton
@Slf4j
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
public class SamlBasedAuthHandler implements AuthHandler {
  @Inject private SamlClientService samlClientService;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private SamlUserGroupSync samlUserGroupSync;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private DomainWhitelistCheckerService domainWhitelistCheckerService;
  @Inject private NgSamlAuthorizationEventPublisher ngSamlAuthorizationEventPublisher;

  @Override
  public AuthenticationResponse authenticate(String... credentials) {
    try {
      if (credentials == null || !ImmutableList.of(2, 3, 4).contains(credentials.length)) {
        log.error(
            "Wrong number of arguments to saml authentication - " + (credentials == null ? 0 : credentials.length));
        throw new WingsException("Invalid arguments while authenticating using SAML");
      }
      String idpUrl = credentials[0];
      String samlResponseString = credentials[1];
      String accountId = credentials.length >= 3 ? credentials[2] : null;
      String relayState = credentials.length >= 4 ? credentials[3] : "";
      Map<String, String> relayStateData = getRelayStateData(relayState);

      User user = decodeResponseAndReturnUser(idpUrl, samlResponseString, accountId);
      accountId = StringUtils.isEmpty(accountId) ? (user == null ? null : user.getDefaultAccountId()) : accountId;
      String uuid = user == null ? null : user.getUuid();
      try (AutoLogContext ignore = new UserLogContext(accountId, uuid, OVERRIDE_ERROR)) {
        log.info("Authenticating via SAML");
        Account account = authenticationUtils.getDefaultAccount(user);
        if (!domainWhitelistCheckerService.isDomainWhitelisted(user, account)) {
          domainWhitelistCheckerService.throwDomainWhitelistFilterException();
        }
        log.info("Authenticating via SAML for user in account {}", account.getUuid());
        SamlSettings samlSettings = ssoSettingService.getSamlSettingsByAccountId(account.getUuid());

        // Occurs when SAML settings are being tested before being enabled
        if (!relayStateData.getOrDefault(SAML_TRIGGER_TYPE, "").equals("login")
            && account.getAuthenticationMechanism() != io.harness.ng.core.account.AuthenticationMechanism.SAML) {
          log.info("SAML test login successful for user: [{}]", user.getEmail());
          throw new WingsException(ErrorCode.SAML_TEST_SUCCESS_MECHANISM_NOT_ENABLED);
        }
        if (Objects.nonNull(samlSettings) && samlSettings.isAuthorizationEnabled()) {
          List<String> userGroups = getUserGroupsForIdpUrl(idpUrl, samlResponseString, accountId);
          SamlUserAuthorization samlUserAuthorization =
              SamlUserAuthorization.builder().email(user.getEmail()).userGroups(userGroups).build();

          // Event Publisher
          samlUserGroupSync.syncUserGroup(samlUserAuthorization, account.getUuid(), samlSettings.getUuid());
          try {
            ngSamlAuthorizationEventPublisher.publishSamlAuthorizationAssertion(
                samlUserAuthorization, account.getUuid(), samlSettings.getUuid());
          } catch (Exception e) {
            log.error(
                "Exception in publishing event for SAML Assertion for {} with account {} userGroups {} and SSO {} ",
                user.getEmail(), account.getUuid(), userGroups, samlSettings.getDisplayName());
          }
        }
        return new AuthenticationResponse(user);
      }
    } catch (URISyntaxException e) {
      throw new WingsException("Saml Authentication Failed", e);
    } catch (UnsupportedEncodingException e) {
      throw new WingsException("Saml Authentication Failed while parsing RelayState", e);
    }
  }

  private Map<String, String> getRelayStateData(String relayState) throws UnsupportedEncodingException {
    Map<String, String> relayStateData = new HashMap<>();
    if (StringUtils.isEmpty(relayState)) {
      return relayStateData;
    }

    String[] pairs = relayState.split("&");
    for (String pair : pairs) {
      String[] items = pair.split("=");
      relayStateData.put(URLDecoder.decode(items[0], "UTF-8"), URLDecoder.decode(items[1], "UTF-8"));
    }
    return relayStateData;
  }

  private User decodeResponseAndReturnUser(String idpUrl, String samlResponseString, String accountId)
      throws URISyntaxException {
    String host = samlClientService.getHost(idpUrl);
    HostType hostType = samlClientService.getHostType(idpUrl);
    switch (hostType) {
      case GOOGLE: {
        Iterator<SamlSettings> samlSettingsIterator = samlClientService.getSamlSettingsFromOrigin(host, accountId);
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
              log.warn("Could not validate SAML Response idpUrl:[{}], samlSettings url:[{}]", idpUrl,
                  samlSettings.getUrl(), e);
            }
          }

          // if you have reached here, it means none of the SAML IDP metadata matched, try a brute force approach the
          // 2nd time
          User user = getUserForIdpUrl(idpUrl, samlResponseString, accountId);
          if (user != null) {
            return user;
          }
        }
      } break;
      case AZURE: {
        Iterator<SamlSettings> samlSettingsIterator = samlClientService.getSamlSettingsFromOrigin(host, accountId);
        if (samlSettingsIterator != null) {
          while (samlSettingsIterator.hasNext()) {
            SamlSettings samlSettings = samlSettingsIterator.next();
            if (idpUrl.startsWith(samlSettings.getUrl())) {
              try {
                return getUser(samlResponseString, samlSettings);
              } catch (SamlException e) {
                log.warn("Could not validate SAML Response idpUrl:[{}], samlSettings url:[{}]", idpUrl,
                    samlSettings.getUrl(), e);
              }
            }
          }
          // if you have reached here, it means none of the SAML IDP metadata matched, try a brute force approach the
          // 2nd time
          User user = getUserForIdpUrl(idpUrl, samlResponseString, accountId);
          if (user != null) {
            return user;
          }
        }
      }

      break;
      default: {
        User user = getUserForIdpUrl(idpUrl, samlResponseString, accountId);
        if (user != null) {
          return user;
        }
      }
    }

    log.info("No IDP metadata could be found for URL [{}]", idpUrl);
    throw new WingsException("Saml Authentication Failed");
  }

  private User getUserForIdpUrl(String idpUrl, String samlResponseString, String accountId) throws URISyntaxException {
    String host = new URI(idpUrl).getHost();
    Iterator<SamlSettings> samlSettingsIterator = samlClientService.getSamlSettingsFromOrigin(host, accountId);
    if (samlSettingsIterator != null) {
      while (samlSettingsIterator.hasNext()) {
        SamlSettings samlSettings = samlSettingsIterator.next();
        try {
          return getUser(samlResponseString, samlSettings);
        } catch (SamlException e) {
          log.warn(
              "Could not validate SAML Response idpUrl:[{}], samlSettings url:[{}]", idpUrl, samlSettings.getUrl(), e);
        }
      }
    }
    return null;
  }

  private List<String> getUserGroupsForIdpUrl(String idpUrl, String samlResponseString, String accountId)
      throws URISyntaxException {
    String host = new URI(idpUrl).getHost();
    Iterator<SamlSettings> samlSettingsIterator = samlClientService.getSamlSettingsFromOrigin(host, accountId);
    if (samlSettingsIterator != null) {
      while (samlSettingsIterator.hasNext()) {
        SamlSettings samlSettings = samlSettingsIterator.next();
        try {
          return getUserGroups(samlResponseString, samlSettings);
        } catch (Exception e) {
          // try decoding again with the next saml settings.
        }
      }
    }
    log.info("Authorization failed for Saml with idp URL [{}]", idpUrl);
    throw new WingsException("Saml Authorization Failed");
  }

  private List<String> getUserGroups(String samlResponseString, SamlSettings samlSettings) throws SamlException {
    List<String> userGroups = new ArrayList<>();
    SamlClient samlClient = samlClientService.getSamlClient(samlSettings);
    SamlResponse samlResponse = samlClient.decodeAndValidateSamlResponse(samlResponseString);
    String groupMembershipAttr = samlSettings.getGroupMembershipAttr();
    List<AttributeStatement> attributeStatements = samlResponse.getAssertion().getAttributeStatements();

    for (AttributeStatement attributeStatement : attributeStatements) {
      for (Attribute attribute : attributeStatement.getAttributes()) {
        if (attribute.getName().equals(groupMembershipAttr)) {
          for (XMLObject xmlObject : attribute.getAttributeValues()) {
            final String userGroup = getAttributeValue(xmlObject);
            if (userGroup != null) {
              userGroups.add(userGroup);
            }
          }
        }
      }
    }
    return userGroups;
  }

  private String getAttributeValue(XMLObject attributeValue) {
    if (attributeValue == null) {
      return null;
    } else {
      if (attributeValue instanceof XSString) {
        return getStringAttributeValue((XSString) attributeValue);
      } else if (attributeValue instanceof XSAnyImpl) {
        return getAnyAttributeValue((XSAnyImpl) attributeValue);
      } else {
        return attributeValue.toString();
      }
    }
  }

  private String getStringAttributeValue(XSString attributeValue) {
    return attributeValue.getValue();
  }

  private String getAnyAttributeValue(XSAnyImpl attributeValue) {
    return attributeValue.getTextContent();
  }

  private User getUser(String samlResponseString, SamlSettings samlSettings) throws SamlException {
    SamlClient samlClient = samlClientService.getSamlClient(samlSettings);
    SamlResponse samlResponse = samlClient.decodeAndValidateSamlResponse(samlResponseString);
    String nameId = samlResponse.getNameID();
    try {
      User user = authenticationUtils.getUser(nameId);
      validateUser(user, samlSettings.getAccountId());
      return user;
    } catch (WingsException e) {
      log.warn("SamlResponse contains nameId=[{}] which does not exist in db, url=[{}], accountId=[{}]", nameId,
          samlSettings.getUrl(), samlSettings.getAccountId());
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST, e);
    }
  }

  // TODO : revisit this method when we are doing SAML authorization
  protected void validateUser(User user, String accountId) {
    if (user.getAccounts().parallelStream().filter(account -> account.getUuid().equals(accountId)).count() == 0) {
      log.warn("User : [{}] not part of accountId : [{}]", user.getEmail(), accountId);
      throw new WingsException(ErrorCode.ACCESS_DENIED);
    }
  }

  @Override
  public io.harness.ng.core.account.AuthenticationMechanism getAuthenticationMechanism() {
    return AuthenticationMechanism.SAML;
  }
}

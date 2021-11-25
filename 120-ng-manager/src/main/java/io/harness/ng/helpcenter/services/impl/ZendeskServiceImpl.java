package io.harness.ng.helpcenter.services.impl;

import static com.google.common.base.Charsets.UTF_8;

import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.NextGenConfiguration;
import io.harness.ng.helpcenter.services.ZendeskService;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.Principal;
import io.harness.security.dto.PrincipalType;
import io.harness.security.dto.UserPrincipal;

import software.wings.beans.User.UserKeys;
import software.wings.beans.ZendeskSsoLoginResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Date;
import java.util.UUID;

@Singleton
public class ZendeskServiceImpl implements ZendeskService {
  @Inject NextGenConfiguration nextGenConfiguration;

  @Override
  public ZendeskSsoLoginResponse generateZendeskSsoJwt(String returnToUrl) {
    String jwtZendeskSecret = nextGenConfiguration.getNextGenConfig().getJwtZendeskSecret();
    if (jwtZendeskSecret == null) {
      throw new InvalidRequestException("Request can not be completed. No Zendesk SSO secret found.");
    }

    Principal principal = SecurityContextBuilder.getPrincipal();
    if (!PrincipalType.USER.equals(principal.getType())) {
      throw new InvalidRequestException("Unable to generate zendesk sso");
    }
    UserPrincipal userPrincipal = (UserPrincipal) principal;

    // Given a user instance
    JWTClaimsSet jwtClaims = new JWTClaimsSet.Builder()
                                 .issueTime(new Date())
                                 .jwtID(UUID.randomUUID().toString())
                                 .claim(UserKeys.name, userPrincipal.getName())
                                 .claim(UserKeys.email, userPrincipal.getEmail())
                                 .build();

    // Create JWS header with HS256 algorithm
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256).contentType("text/plain").build();

    // Create JWS object
    JWSObject jwsObject = new JWSObject(header, new Payload(jwtClaims.toJSONObject()));

    try {
      // Create HMAC signer
      JWSSigner signer = new MACSigner(jwtZendeskSecret.getBytes(UTF_8));
      jwsObject.sign(signer);
    } catch (com.nimbusds.jose.JOSEException e) {
      throw new InvalidRequestException("Error signing JWT: " + ExceptionUtils.getMessage(e));
    }

    // Serialise to JWT compact form
    String jwtString = jwsObject.serialize();

    String redirectUrl =
        String.format("%s/access/jwt?jwt=%s", nextGenConfiguration.getZendeskConfig().getBaseUrl(), jwtString);

    if (returnToUrl != null) {
      redirectUrl += "&return_to=" + returnToUrl;
    }
    return ZendeskSsoLoginResponse.builder().redirectUrl(redirectUrl).userId(userPrincipal.getName()).build();
  }
}

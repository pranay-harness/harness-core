package io.harness.marketplace.gcp;

import static io.harness.eraro.ErrorCode.INVALID_TOKEN;
import static io.harness.exception.WingsException.USER;
import static io.harness.marketplace.gcp.GcpMarketPlaceConstants.ISSUER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nimbusds.jose.jwk.JWKSet;
import io.harness.configuration.DeployMode;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.marketplace.gcp.signup.GcpMarketplaceSignUpHandler;
import io.harness.marketplace.gcp.signup.annotations.NewSignUp;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import software.wings.app.MainConfiguration;
import software.wings.security.authentication.AuthenticationUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.interfaces.RSAKey;
import java.text.ParseException;
import javax.ws.rs.core.Response;

@Slf4j
@Singleton
public class GcpMarketPlaceApiHandlerImpl implements GcpMarketPlaceApiHandler {
  @Inject private MainConfiguration configuration;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject @NewSignUp private GcpMarketplaceSignUpHandler newUserSignUpHandler;

  @Override
  public Response signUp(final String token) {
    logger.info("GCP Marketplace register request. Token: {}", token);
    if (DeployMode.isOnPrem(configuration.getDeployMode().name())) {
      logger.error("GCP MarketPlace is disabled in On-Prem");
      return redirectToErrorPage();
    }
    // TODO: Verify Gcp Marketplace Token here

    return redirectTo(newUserSignUpHandler.signUp(token));
  }

  private static Response redirectTo(URI uri) {
    return Response.seeOther(uri).build();
  }

  private Response redirectToErrorPage() {
    try {
      String baseUrl = authenticationUtils.getBaseUrl() + "#/fallback";
      URI redirectUrl = new URIBuilder(baseUrl).addParameter("type", "GCP_ON_PREMISE_ERROR").build();
      return Response.seeOther(redirectUrl).build();
    } catch (URISyntaxException e) {
      throw new WingsException(e);
    }
  }

  private void verifyGcpMarketplaceToken(final String gcpMarketplaceToken) {
    try {
      JWKSet jwks = JWKSet.load(new URL(ISSUER));
      JWT decodedToken = JWT.decode(gcpMarketplaceToken);
      RSAKey rsaKey = (RSAKey) jwks.getKeyByKeyId(decodedToken.getKeyId());
      Algorithm algorithm = Algorithm.RSA256(rsaKey);
      JWTVerifier verifier =
          JWT.require(algorithm).withIssuer(ISSUER).withClaim("aud", GcpMarketPlaceConstants.PROJECT_ID).build();
      verifier.verify(gcpMarketplaceToken);
    } catch (JWTVerificationException e) {
      throw new InvalidRequestException("Failed to verify JWT Token", e, INVALID_TOKEN, USER);
    } catch (ParseException | IOException e) {
      throw new GeneralException("Failed to form URL from " + ISSUER, e);
    }
  }
}

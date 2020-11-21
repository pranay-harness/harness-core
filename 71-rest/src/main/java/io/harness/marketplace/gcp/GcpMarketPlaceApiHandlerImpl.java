package io.harness.marketplace.gcp;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.exception.WingsException.USER;
import static io.harness.marketplace.gcp.GcpMarketPlaceConstants.TOKEN_AUDIENCE;
import static io.harness.marketplace.gcp.GcpMarketPlaceConstants.TOKEN_ISSUER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.configuration.DeployMode;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.FailureType;
import io.harness.exception.GeneralException;
import io.harness.marketplace.gcp.signup.GcpMarketplaceSignUpHandler;
import io.harness.marketplace.gcp.signup.annotations.NewSignUp;

import software.wings.app.MainConfiguration;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.service.intfc.signup.SignupException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.EnumSet;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;

@OwnedBy(PL)
@Slf4j
@Singleton
public class GcpMarketPlaceApiHandlerImpl implements GcpMarketPlaceApiHandler {
  @Inject private MainConfiguration configuration;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject @NewSignUp private GcpMarketplaceSignUpHandler newUserSignUpHandler;

  @Override
  public Response signUp(final String token) {
    if (DeployMode.isOnPrem(configuration.getDeployMode().name())) {
      log.error("GCP MarketPlace is disabled in On-Prem");
      return redirectToErrorPage();
    }
    verifyGcpMarketplaceToken(token);
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
      throw new SignupException(
          null, e, ErrorCode.UNEXPECTED, Level.ERROR, USER, EnumSet.of(FailureType.APPLICATION_ERROR));
    }
  }

  private void verifyGcpMarketplaceToken(String token) {
    DecodedJWT jwt = JWT.decode(token);
    log.info("GCP Marketplace registration request. gcpAccountId: {}", jwt.getSubject());
    try {
      HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
      JsonParser jsonParser = new JsonParser();

      // Fetch certificate from issuer URL
      GenericUrl issuerUrl = new GenericUrl(TOKEN_ISSUER);
      HttpRequest request = requestFactory.buildGetRequest(issuerUrl);
      String certsJson = request.execute().parseAsString();

      // Lookup certificate by kid
      JsonElement jsonRoot = jsonParser.parse(certsJson);
      String certificateStr = jsonRoot.getAsJsonObject().get(jwt.getKeyId()).getAsString();

      // Get public key from certificate
      InputStream is = new ByteArrayInputStream(certificateStr.getBytes());
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      Certificate cert = cf.generateCertificate(is);
      RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

      // Build JWT Verifier
      Algorithm algorithm = Algorithm.RSA256(publicKey);
      JWTVerifier verifier = JWT.require(algorithm).withIssuer(TOKEN_ISSUER).withAudience(TOKEN_AUDIENCE).build();

      // Verify token
      verifier.verify(token);

    } catch (Exception e) {
      throw new GeneralException("Failed to verify GCP Marketplace token", e);
    }
  }
}

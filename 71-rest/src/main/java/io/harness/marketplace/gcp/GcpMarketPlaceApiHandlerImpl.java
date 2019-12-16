package io.harness.marketplace.gcp;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.JWT;
import io.harness.exception.WingsException;
import io.harness.marketplace.gcp.signup.GcpMarketplaceSignUpHandler;
import io.harness.marketplace.gcp.signup.annotations.NewSignUp;
import io.harness.marketplace.gcp.signup.annotations.ReturningUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.MarketPlace;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.dl.WingsPersistence;
import software.wings.security.authentication.AuthenticationUtils;
import software.wings.service.intfc.marketplace.MarketPlaceService;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import javax.ws.rs.core.Response;

@Slf4j
@Singleton
public class GcpMarketPlaceApiHandlerImpl implements GcpMarketPlaceApiHandler {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration configuration;
  @Inject private AuthenticationUtils authenticationUtils;
  @Inject private MarketPlaceService marketPlaceService;
  @Inject @NewSignUp private GcpMarketplaceSignUpHandler newUserSignUpHandler;
  @Inject @ReturningUser private GcpMarketplaceSignUpHandler returningUserHandler;

  enum RedirectErrorType { GCP_NO_EVENT_RECEIVED_ERROR, GCP_ON_PREMISE_ERROR }

  @Override
  public Response signUp(final String token) {
    logger.info("GCP Marketplace register request. Token: {}", token);
    if (DeployMode.isOnPrem(configuration.getDeployMode().name())) {
      logger.error(
          "GCP MarketPlace is disabled in On-Prem, please contact Harness at support@harness.io, customertoken= {}",
          token);
      return redirectToErrorPage(RedirectErrorType.GCP_ON_PREMISE_ERROR);
    }

    verifyJWT(token);
    JWT decodedToken = JWT.decode(token);
    String gcpAccountId = decodedToken.getSubject();

    // GCP entitlements don't have expiry date, so setting an expiry of 1 year
    final Instant defaultExpiry = Instant.now().plus(365, ChronoUnit.DAYS);
    MarketPlace newMarketPlace = MarketPlace.builder()
                                     .type(MarketPlaceType.GCP)
                                     .customerIdentificationCode(gcpAccountId)
                                     .token(token)
                                     .expirationDate(Date.from(defaultExpiry))
                                     .orderQuantity(GcpMarketPlaceApiHandler.GCP_PRO_PLAN_ORDER_QUANTITY)
                                     .build();

    Optional<MarketPlace> existingMarketPlace = marketPlaceService.fetchMarketplace(gcpAccountId, MarketPlaceType.GCP);
    MarketPlace marketPlace = existingMarketPlace.orElse(newMarketPlace);
    wingsPersistence.save(marketPlace);

    String harnessAccountId = marketPlace.getAccountId();
    boolean isNewCustomer = null == harnessAccountId;

    URI redirectUrl;
    if (isNewCustomer) {
      redirectUrl = newUserSignUpHandler.signUp(marketPlace);
    } else {
      redirectUrl = returningUserHandler.signUp(marketPlace);
    }

    return redirectTo(redirectUrl);
  }

  private static Response redirectTo(URI uri) {
    return Response.seeOther(uri).build();
  }

  private Response redirectToErrorPage(RedirectErrorType type) {
    try {
      String baseUrl = authenticationUtils.getBaseUrl() + "#/fallback";
      URI redirectUrl = new URIBuilder(baseUrl).addParameter("type", type.toString()).build();
      return Response.seeOther(redirectUrl).build();
    } catch (URISyntaxException e) {
      throw new WingsException(e);
    }
  }

  // TODO(jatin): implement this
  private void verifyJWT(final String token) {}
}

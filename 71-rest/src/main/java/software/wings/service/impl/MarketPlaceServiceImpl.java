package software.wings.service.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.marketplaceentitlement.AWSMarketplaceEntitlementClient;
import com.amazonaws.services.marketplaceentitlement.model.GetEntitlementsRequest;
import com.amazonaws.services.marketplaceentitlement.model.GetEntitlementsResult;
import com.amazonaws.services.marketplacemetering.AWSMarketplaceMeteringClientBuilder;
import com.amazonaws.services.marketplacemetering.model.AWSMarketplaceMeteringException;
import com.amazonaws.services.marketplacemetering.model.ResolveCustomerRequest;
import com.amazonaws.services.marketplacemetering.model.ResolveCustomerResult;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.LicenseInfo;
import software.wings.beans.MarketPlace;
import software.wings.beans.UserInvite;
import software.wings.beans.marketplace.MarketPlaceConstants;
import software.wings.beans.marketplace.MarketPlaceType;
import software.wings.dl.WingsPersistence;
import software.wings.licensing.LicenseService;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.security.authentication.AuthenticationUtil;
import software.wings.security.authentication.MarketPlaceConfig;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.MarketPlaceService;
import software.wings.service.intfc.UserService;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;

@Singleton
@Slf4j
public class MarketPlaceServiceImpl implements MarketPlaceService {
  @Inject private AccountService accountService;
  @Inject private UserService userService;
  @Inject private MainConfiguration configuration;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private LicenseService licenseService;
  @Inject private SecretManager secretManager;
  @Inject private AuthenticationUtil authenticationUtil;

  private static final String INFO = "INFO";
  private static final String REDIRECT_ACTION_LOGIN = "LOGIN";
  private final String MESSAGESTATUS = "SUCCESS";
  @Override
  public Response processAWSMarktPlaceOrder(String token) {
    /**
     * If request gets routed to the free cluster, reject the request rightaway
     */
    if (configuration.isTrialRegistrationAllowed()) {
      final String message = "Invalid cluster, please contact Harness at support@harness.io, customertoken=" + token;
      return generateMessageResponse(message, "ERROR", null, null);
    }

    if (DeployMode.isOnPrem(configuration.getDeployMode().name())) {
      final String message =
          "MarketPlace is disabled in On-Prem, please contact Harness at support@harness.io, customertoken=" + token
          + ", deploymode=" + configuration.getDeployMode();
      return generateMessageResponse(message, "ERROR", null, null);
    }

    ResolveCustomerRequest resolveCustomerRequest = new ResolveCustomerRequest().withRegistrationToken(token);
    final MarketPlaceConfig marketPlaceConfig = configuration.getMarketPlaceConfig();

    BasicAWSCredentials awsCreds =
        new BasicAWSCredentials(marketPlaceConfig.getAwsAccessKey(), marketPlaceConfig.getAwsSecretKey());

    ResolveCustomerResult resolveCustomerResult;
    try {
      resolveCustomerResult = AWSMarketplaceMeteringClientBuilder.standard()
                                  .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                                  .withRegion(Regions.US_EAST_1)
                                  .build()
                                  .resolveCustomer(resolveCustomerRequest);
    } catch (AWSMarketplaceMeteringException e) {
      logger.error("Failed to resolveCustomer for customerToken:[{}]", token, e);
      return generateMessageResponse("Failed to authenticate user with AWS", "ERROR", null, null);
    }
    if (null == resolveCustomerResult) {
      final String message =
          "Customer order from AWS could not be resolved, please contact Harness at support@harness.io" + token;
      logger.error(message);
      return generateMessageResponse(message, "ERROR", null, null);
    }
    logger.info("ResolveCustomerResult=[{}]", resolveCustomerResult);

    String customerIdentifierCode = resolveCustomerResult.getCustomerIdentifier();
    String productCode = resolveCustomerResult.getProductCode();

    if (!marketPlaceConfig.getAwsMarketPlaceProductCode().equals(productCode)) {
      final String message =
          "Customer order from AWS could not be resolved, please contact Harness at support@harness.io";
      logger.error("Invalid AWS productcode received:[{}],", productCode);
      return generateMessageResponse(message, "ERROR", null, null);
    }

    GetEntitlementsRequest entitlementRequest = new GetEntitlementsRequest();
    entitlementRequest.setProductCode(productCode);
    Map<String, List<String>> entitlementFilter = new HashMap();

    List<String> customerIdentifier = new ArrayList();
    customerIdentifier.add(customerIdentifierCode);
    entitlementFilter.put("CUSTOMER_IDENTIFIER", customerIdentifier);
    entitlementRequest.setFilter(entitlementFilter);

    AWSMarketplaceEntitlementClient oClient =
        (AWSMarketplaceEntitlementClient) AWSMarketplaceEntitlementClient.builder()
            .withRegion(Regions.US_EAST_1)
            .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
            .build();

    GetEntitlementsResult entitlements = oClient.getEntitlements(entitlementRequest);
    logger.info("oEntitlementResult=[{}]", entitlements);
    Integer orderQuantity = getOrderQuantity(entitlements.getEntitlements().get(0).getDimension());
    Date expirationDate = entitlements.getEntitlements().get(0).getExpirationDate();
    MarketPlace marketPlace = wingsPersistence.createQuery(MarketPlace.class)
                                  .field("type")
                                  .equal(MarketPlaceType.AWS)
                                  .field("customerIdentificationCode")
                                  .equal(customerIdentifierCode)
                                  .get();

    boolean existingCustomer = false;
    if (marketPlace != null) {
      logger.info("Existing customer, not creating a new account");
      if (marketPlace.getAccountId() != null) {
        existingCustomer = true;
      } else {
        logger.info(
            "MarketPlace customer:[{}] does not have an account associated with him, will treat him as a new customer",
            customerIdentifierCode);
      }
    } else {
      marketPlace = MarketPlace.builder()
                        .type(MarketPlaceType.AWS)
                        .customerIdentificationCode(customerIdentifierCode)
                        .token(token)
                        .orderQuantity(orderQuantity)
                        .expirationDate(expirationDate)
                        .build();
      wingsPersistence.save(marketPlace);
    }

    if (existingCustomer && (!marketPlace.getOrderQuantity().equals(orderQuantity))
        || (!marketPlace.getExpirationDate().equals(expirationDate))) {
      logger.info(
          "This is an existing customer:[{}], updating orderQuantity from [{}] to [{}], updating expirationDate from [{}] to [{}]",
          customerIdentifierCode, marketPlace.getOrderQuantity(), orderQuantity, marketPlace.getExpirationDate(),
          expirationDate);
      /**
       * This is an update to an existing order, treat this as an update
       */
      licenseService.updateAccountLicense(marketPlace.getAccountId(),
          LicenseInfo.builder()
              .accountType(AccountType.PAID)
              .licenseUnits(orderQuantity)
              .accountStatus(AccountStatus.ACTIVE)
              .expiryTime(expirationDate.getTime())
              .build());

      marketPlace.setOrderQuantity(orderQuantity);
      wingsPersistence.save(marketPlace);

      final String message = String.format("License details: Service Instances: %d, License expiration: %s",
          orderQuantity, DateFormat.getDateInstance(DateFormat.SHORT).format(expirationDate));
      return generateMessageResponse(message, INFO, REDIRECT_ACTION_LOGIN, MESSAGESTATUS);

    } else if (!existingCustomer) {
      /**
       * This is a brand new customer
       */

      UserInvite userInvite = userService.createUserInviteForMarketPlace();

      String marketPlaceToken = getMarketPlaceToken(marketPlace, userInvite);

      URI redirectUrl = null;
      try {
        redirectUrl = new URI(authenticationUtil.getBaseUrl()
            + ("#/invite?inviteId=" + userInvite.getUuid() + "&marketPlaceToken=" + marketPlaceToken));
      } catch (URISyntaxException e) {
        throw new WingsException(e);
      }
      return Response.seeOther(redirectUrl).build();

    } else {
      final String message = String.format("License details: Service Instances: %d, License expiration: %s",
          orderQuantity, DateFormat.getDateInstance(DateFormat.SHORT).format(expirationDate));
      return generateMessageResponse(message, INFO, REDIRECT_ACTION_LOGIN, MESSAGESTATUS);
    }
  }

  @VisibleForTesting
  public String getMarketPlaceToken(MarketPlace marketPlace, UserInvite userInvite) {
    Map<String, String> claims = new HashMap<>();
    claims.put(MarketPlaceConstants.USERINVITE_ID_CLAIM_KEY, userInvite.getUuid());
    claims.put(MarketPlaceConstants.MARKETPLACE_ID_CLAIM_KEY, marketPlace.getUuid());
    return secretManager.generateJWTToken(claims, JWT_CATEGORY.MARKETPLACE_SIGNUP);
  }

  private Response generateMessageResponse(String message, String type, String action, String status) {
    URI redirectUrl = null;
    try {
      redirectUrl = new URI(authenticationUtil.getBaseUrl()
          + "#/fallback?message=" + URLEncoder.encode(message, "UTF-8") + "&type=" + type
          + (action != null ? "&action=" + action : "") + (status != null ? "&status=" + status : ""));
    } catch (URISyntaxException | UnsupportedEncodingException e) {
      throw new WingsException(e);
    }
    return Response.seeOther(redirectUrl).build();
  }

  private Integer getOrderQuantity(String dimension) {
    switch (dimension) {
      case MarketPlaceConstants.AWS_MARKETPLACE_50_INSTANCES:
        return 50;
      case MarketPlaceConstants.AWS_MARKETPLACE_200_INSTANCES:
        return 200;
      case MarketPlaceConstants.AWS_MARKETPLACE_500_INSTANCES:
        return 500;
      case MarketPlaceConstants.AWS_MARKETPLACE_750_INSTANCES:
        return 750;
      case MarketPlaceConstants.AWS_MARKETPLACE_1000_INSTANCES:
        return 1000;
      case MarketPlaceConstants.AWS_MARKETPLACE_1500_INSTANCES:
        return 1500;
      case MarketPlaceConstants.AWS_MARKETPLACE_2500_INSTANCES:
        return 2500;
      default:
        return 50;
    }
  }
}

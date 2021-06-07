package io.harness.licensing.api.resource;

import io.harness.licensing.Edition;
import io.harness.licensing.LicenseStatus;
import io.harness.licensing.LicenseType;
import io.harness.licensing.ModuleType;
import io.harness.licensing.entities.account.AccountLicense;
import io.harness.licensing.entities.transactions.LicenseTransaction;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.repositories.AccountLicenseRepository;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import io.swagger.annotations.Api;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import retrofit2.http.Body;

@Api("/payment/")
@Path("/payment/")
@NextGenManagerAuth
@Slf4j
public class PaymentResource {
  private static final String API_KEY =
      "sk_test_51IzYnIK1vsc7tc8y6jZOJZyWHApHYNQJdcfCYKkhkxM4fHz1VnyzoHb3UpgqEUSeKuNQQPWnJfJcw0OufgJlnCLa00Pk2IGjvr";
  private static Gson gson = new Gson();
  @Inject private AccountLicenseRepository accountLicenseRepository;

  static {
    Stripe.apiKey = API_KEY;
  }

  @GET
  @Produces("application/json")
  @Path("setup")
  @PublicApi
  public String setup() {
    Map<String, Object> responseData = new HashMap<>();
    responseData.put("publishableKey",
        "pk_test_51IzYnIK1vsc7tc8yri9N5swmDxflQhIkAQbVxepcq1YoRlvUDgCJNGaoPCcSqyfOQt5tyRy4a3OBhezumJpPtm3100xKIx3aZ6");
    responseData.put("CI Enterprise", "price_1J06w4K1vsc7tc8ybyMPCr0f");
    responseData.put("CD Enterprise", "price_1IzkdEK1vsc7tc8yfH9I4Grh");
    return gson.toJson(responseData);
  }

  @GET
  @Produces("application/json")
  @Path("session")
  @PublicApi
  public String getSesssion(@QueryParam("sessionId") String sessionId) throws StripeException {
    Session session = Session.retrieve(sessionId);
    return gson.toJson(session);
  }

  @POST
  @Produces("application/json")
  @Path("create-checkout-session")
  @PublicApi
  public String createCheckoutSession(@NotNull @Valid @Body CreateCheckoutSessionRequest req) throws StripeException {
    String domainUrl = "https://localhost:8181/";

    // Create new Checkout Session for the order
    // Other optional params include:
    // [billing_address_collection] - to display billing address details on the page
    // [customer] - if you have an existing Stripe Customer ID
    // [payment_intent_data] - lets capture the payment later
    // [customer_email] - lets you prefill the email input in the form
    // For full details see https://stripe.com/docs/api/checkout/sessions/create

    // ?session_id={CHECKOUT_SESSION_ID} means the redirect will have the session ID
    // set as a query param
    SessionCreateParams params = new SessionCreateParams.Builder()
                                     .setCustomerEmail(req.getEmail())
                                     .setSuccessUrl(domainUrl + "#/account/" + req.getAccountId()
                                         + "/home/setup/subscriptions?session_id={CHECKOUT_SESSION_ID}&succeed=true")
                                     .setCancelUrl(domainUrl + "/canceled.html")
                                     .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                                     .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                                     .setClientReferenceId(req.getAccountId())
                                     .addLineItem(new SessionCreateParams.LineItem.Builder()
                                                      .setQuantity(Long.valueOf(req.getQuantity()))
                                                      .setPrice(req.getPriceId())
                                                      .build())
                                     .build();

    try {
      Session session = Session.create(params);
      Map<String, Object> responseData = new HashMap<>();
      responseData.put("sessionId", session.getId());
      return gson.toJson(responseData);
    } catch (Exception e) {
      Map<String, Object> messageData = new HashMap<>();
      messageData.put("message", e.getMessage());
      Map<String, Object> responseData = new HashMap<>();
      responseData.put("error", messageData);
      return gson.toJson(responseData);
    }
  }

  @POST
  @Produces("application/json")
  @Path("webhook")
  @PublicApi
  public String webHook(@HeaderParam("Stripe-Signature") String sigHeader, @NotNull @Valid @Body String payload) {
    String endpointSecret = "whsec_iR5nK9mVSqlHoxxe56Ouxcwk8YNOt35Q";

    Event event = null;

    try {
      event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
    } catch (SignatureVerificationException e) {
      // Invalid signature
      log.error("invalid signature", e);
      return "";
    }

    switch (event.getType()) {
      case "checkout.session.completed":
        // Payment is successful and the subscription is created.
        // You should provision the subscription and save the customer ID to your database.
        log.info("checkout.session.completed");
        Optional<StripeObject> object = event.getDataObjectDeserializer().getObject();
        Session session = (Session) object.get();
        AccountLicense accountLicense =
            accountLicenseRepository.findByAccountIdentifier(session.getClientReferenceId());
        List<LicenseTransaction> transactions = accountLicense.getModuleLicenses().get(ModuleType.CI).getTransactions();
        CILicenseTransaction transaction =
            CILicenseTransaction.builder()
                .startTime(Instant.now().toEpochMilli())
                .developers(session.getLineItems().getData().get(0).getQuantity().intValue())
                .status(LicenseStatus.ACTIVE)
                .accountIdentifier(session.getCustomer())
                .moduleType(ModuleType.CI)
                .edition(Edition.ENTERPRISE)
                .licenseType(LicenseType.PAID)
                .uuid(ObjectId.get().toHexString())
                .expiryTime(Instant.now().plus(1, ChronoUnit.YEARS).toEpochMilli())
                .build();
        transactions.add(transaction);
        accountLicenseRepository.save(accountLicense);

        break;
      case "invoice.paid":
        // Continue to provision the subscription as payments continue to be made.
        // Store the status in your database and check when a user accesses your service.
        // This approach helps you avoid hitting rate limits.
        log.info("invoice.paid");
        break;
      case "invoice.payment_failed":
        // The payment failed or the customer does not have a valid payment method.
        // The subscription becomes past_due. Notify your customer and send them to the
        // customer portal to update their payment information.
        log.info("invoice.payment_failed");
        break;
      default:
        log.info("Unhandled event type: " + event.getType());
        // System.out.println("Unhandled event type: " + event.getType());
    }
    return "";
  }
}

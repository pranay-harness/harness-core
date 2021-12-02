package io.harness.payment.resources;

import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.payment.beans.CreateCheckoutSessionDTO;
import io.harness.payment.services.CustomerRecord;
import io.harness.payment.services.PaymentService;
import io.harness.payment.services.SubscriptionItem;
import io.harness.payment.services.SubscriptionRecord;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.security.annotations.PublicApi;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import retrofit2.http.Body;

@Api("payment")
@Path("payment")
@Produces({"application/json"})
@Consumes({"application/json"})
@NextGenManagerAuth
@Slf4j
public class PaymentResource {
  private static final String API_KEY =
      "sk_test_51IykZ0Iqk5P9Eha3lDEdilJNRufZDw6xXt79RT39C5RhwDLAV2IZvvSsdqZV8q9HvENsVmAnc1MITqrLap8ElwxH00EJp5orGK";
  private static Gson gson = new Gson();
  @Inject private LicenseService licenseService;
  @Inject private PaymentService paymentService;

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
  @Path("customer")
  @PublicApi
  public ResponseDTO<Customer> getCustomer(@QueryParam("id") String id) {
    Customer customerRecord = paymentService.getCustomerRecord(id);
    return ResponseDTO.newResponse(customerRecord);
  }

  @POST
  @Produces("application/json")
  @Path("customer")
  @PublicApi
  public ResponseDTO<CustomerRecord> createCustomer() {
    CustomerRecord customerRecord =
        CustomerRecord.builder().customerName("test").accountId("test1").email("zhuo.yin@harness.io").build();
    CustomerRecord result = paymentService.createCustomer(customerRecord);
    return ResponseDTO.newResponse(result);
  }

  @PUT
  @Produces("application/json")
  @Path("customer")
  @PublicApi
  public ResponseDTO<CustomerRecord> updateCustomer(@QueryParam("id") String id) {
    CustomerRecord customerRecord =
        CustomerRecord.builder().id(id).customerName("test23").accountId("test1").email("zhuo.yin@harness.io").build();
    CustomerRecord result = paymentService.updateCustomer(customerRecord);
    return ResponseDTO.newResponse(result);
  }

  @DELETE
  @Produces("application/json")
  @Path("customer")
  @PublicApi
  public ResponseDTO<CustomerRecord> deleteCustomer(@QueryParam("id") String id) {
    CustomerRecord customerRecord = paymentService.deleteCustomer(id);
    return ResponseDTO.newResponse(customerRecord);
  }

  @GET
  @Produces("application/json")
  @Path("subscription")
  @PublicApi
  public ResponseDTO<Subscription> retrieveSubscription(@QueryParam("id") String id) {
    Subscription subscription = paymentService.retrieveSubscription(id);
    return ResponseDTO.newResponse(subscription);
  }

  @POST
  @Produces("application/json")
  @Path("subscription")
  @PublicApi
  public ResponseDTO<Subscription> createSubscription(@QueryParam("id") String customerId) {
    SubscriptionRecord subscriptionRecord =
        SubscriptionRecord.builder()
            .customerId(customerId)
            .items(Lists.newArrayList(
                SubscriptionItem.builder().priceId("price_1K4AgOIqk5P9Eha3bFbzoLCl").quantity(26L).build(),
                SubscriptionItem.builder().priceId("price_1K4AhSIqk5P9Eha3cH8pI2V0").quantity(1L).build()))
            .build();

    Subscription result = paymentService.createSubscription(customerId, subscriptionRecord);
    return ResponseDTO.newResponse(result);
  }

  @PUT
  @Produces("application/json")
  @Path("subscription")
  @PublicApi
  public ResponseDTO<Subscription> updateSubscription(@QueryParam("id") String customerId) {
    SubscriptionRecord subscription =
        SubscriptionRecord.builder()
            .customerId(customerId)
            .items(Lists.newArrayList(
                SubscriptionItem.builder().priceId("price_1K4AgOIqk5P9Eha3bFbzoLCl").quantity(35L).build()))
            .build();
    Subscription result = paymentService.updateSubscription(customerId, subscription);
    return ResponseDTO.newResponse(result);
  }

  @DELETE
  @Produces("application/json")
  @Path("subscription")
  @PublicApi
  public ResponseDTO<Boolean> cancelSubscription(@QueryParam("id") String id) {
    paymentService.cancelSubscription(id);
    return ResponseDTO.newResponse(true);
  }

  @PUT
  @Produces("application/json")
  @Path("preview")
  @PublicApi
  public ResponseDTO<Invoice> previewInvoice(
      @QueryParam("id") String subscriptionId, SubscriptionRecord subscriptionRecord) {
    Invoice invoice = paymentService.previewInvoice(subscriptionId, subscriptionRecord);
    return ResponseDTO.newResponse(invoice);
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
  public String createCheckoutSession(@NotNull @Valid @Body CreateCheckoutSessionDTO req) throws StripeException {
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
        //                AccountLicense accountLicense =
        //                        accountLicenseRepository.findByAccountIdentifier(session.getClientReferenceId());
        //                List<LicenseTransaction> transactions =
        //                accountLicense.getModuleLicenses().get(ModuleType.CI).getTransactions(); CILicenseTransaction
        //                transaction =
        //                        CILicenseTransaction.builder()
        //                                .startTime(Instant.now().toEpochMilli())
        //                                .developers(session.getLineItems().getData().get(0).getQuantity().intValue())
        //                                .status(LicenseStatus.ACTIVE)
        //                                .accountIdentifier(session.getCustomer())
        //                                .moduleType(ModuleType.CI)
        //                                .edition(Edition.ENTERPRISE)
        //                                .licenseType(LicenseType.PAID)
        //                                .uuid(ObjectId.get().toHexString())
        //                                .expiryTime(Instant.now().plus(1, ChronoUnit.YEARS).toEpochMilli())
        //                                .build();
        //                transactions.add(transaction);
        //                accountLicenseRepository.save(accountLicense);

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

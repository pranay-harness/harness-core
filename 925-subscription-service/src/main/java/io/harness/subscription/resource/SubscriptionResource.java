package io.harness.subscription.resource;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.licensing.accesscontrol.LicenseAccessControlPermissions.VIEW_LICENSE_PERMISSION;

import static com.fasterxml.jackson.databind.MapperFeature.PROPAGATE_TRANSIENT_MARKER;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.exception.InvalidRequestException;
import io.harness.licensing.accesscontrol.ResourceTypes;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.subscription.constant.Prices;
import io.harness.subscription.dto.InvoiceDetailDTO;
import io.harness.subscription.dto.PriceCollectionDTO;
import io.harness.subscription.dto.SubscriptionDTO;
import io.harness.subscription.dto.SubscriptionDetailDTO;
import io.harness.subscription.services.SubscriptionService;
import io.harness.subscription.services.impl.StripeResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PriceCollection;
import com.stripe.model.StripeObject;
import com.stripe.model.StripeObjectInterface;
import com.stripe.net.ApiResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api(value = "subscriptions")
@Path("subscriptions")
@Produces({"application/json"})
@Consumes({"application/json"})
@Tag(name = "Subscriptions", description = "This contains APIs related to license subscriptions as defined in Harness")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Hidden
@NextGenManagerAuth
public class SubscriptionResource {
  private static final String PRICES_KEY = "prices";
  @Inject private SubscriptionService subscriptionService;
  private ObjectMapper objectMapper =
      new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS).enable(PROPAGATE_TRANSIENT_MARKER);

  @POST
  @Path("/prices")
  @ApiOperation(value = "Retrieve product prices", nickname = "retrieveProductPrices")
  @Operation(operationId = "retrieveProductPrices", summary = "Retrieve product prices",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns product prices")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<PriceCollectionDTO>
  retrieveProductPrices(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                            NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true, description = "This is the list of the Prices.") @NotNull List<Prices> prices) {
    return ResponseDTO.newResponse(subscriptionService.listPrices(accountIdentifier, prices));
  }

  @PUT
  @Path("/prices")
  @ApiOperation(value = "Retrieve product prices", nickname = "retrieveProductPrices")
  @Operation(operationId = "retrieveProductPrices", summary = "Retrieve product prices",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns product prices")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<StripeResponse>
  retrieveProductPrices(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                            NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true, description = "This is the list of the Prices.") @NotNull Map<String, Object> params) {
    try {
      return ResponseDTO.newResponse(
          StripeResponse.builder()
              .result(objectMapper.writeValueAsString(subscriptionService.listPrices(accountIdentifier, params)))
              .build());
    } catch (JsonProcessingException e) {
      throw new InvalidRequestException("Unable fetch prices", e);
    }
  }

  @POST
  @Path("/test")
  @ApiOperation(value = "Retrieve product prices", nickname = "retrieveProductPrices")
  @Operation(operationId = "retrieveProductPrices", summary = "Retrieve product prices",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns product prices")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<StripeResponse>
  retrieveProductPrices(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                            NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @QueryParam("method") @AccountIdentifier String method, @QueryParam("path") @AccountIdentifier String path,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true, description = "This is the list of the Prices.") @NotNull Map<String, Object> params) {
    try {
      String url = String.format("%s/v1/%s", Stripe.getApiBase(), path);
      ApiResource.RequestMethod requestMethod = ApiResource.RequestMethod.valueOf(method);
      String text;
      try (Reader reader = new InputStreamReader(ApiResource.requestStream(requestMethod, url, params, null))) {
        JsonNode jsonNode = objectMapper.readTree(reader);
        text = jsonNode.toString();
      }
      return ResponseDTO.newResponse(StripeResponse.builder().result(text).build());
    } catch (StripeException | IOException e) {
      throw new InvalidRequestException("Unable fetch prices", e);
    }
  }

  @POST
  @ApiOperation(value = "Create a subscription", nickname = "createSubscription")
  @Operation(operationId = "createSubscription", summary = "Create a subscription",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns subscription details")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<SubscriptionDetailDTO>
  createSubscription(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                         NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true, description = "This is the details of the Subscription Request.") @NotNull
      @Valid SubscriptionDTO subscriptionDTO) {
    return ResponseDTO.newResponse(subscriptionService.createSubscription(subscriptionDTO));
  }

  @PUT
  @ApiOperation(value = "Update a subscription", nickname = "updateSubscription")
  @Operation(operationId = "updateSubscription", summary = "Update a subscription",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns subscription details")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<SubscriptionDetailDTO>
  updateSubscription(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                         NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true, description = "This is the details of the Subscription Request.") @NotNull
      @Valid SubscriptionDTO subscriptionDTO) {
    return ResponseDTO.newResponse(subscriptionService.updateSubscription(subscriptionDTO));
  }

  @POST
  @Path("/invoices/preview")
  @ApiOperation(value = "Retrieve the upcoming Invoice details", nickname = "retrieveUpcomingInvoice")
  @Operation(operationId = "retrieveUpcomingInvoice", summary = "Retrieve the upcoming Invoice details",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns upcoming invoice")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.LICENSE, permission = VIEW_LICENSE_PERMISSION)
  public ResponseDTO<InvoiceDetailDTO>
  retrieveUpcomingInvoice(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                              NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true, description = "This is the details of the Subscription Request.") @NotNull
      @Valid SubscriptionDTO subscriptionDTO) {
    return ResponseDTO.newResponse(subscriptionService.previewInvoice(subscriptionDTO));
  }
}

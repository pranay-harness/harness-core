package io.harness.ng.webhook;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.webhook.services.api.WebhookService;
import io.harness.security.annotations.InternalApi;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("webhookevent")
@Path("webhookevent")
@Produces({"application/json", "application/yaml", "text/plain"})
@Consumes({"application/json", "application/yaml", "text/plain"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@Slf4j
@OwnedBy(DX)
public class WebhookEventResource {
  private WebhookService webhookService;

  @POST
  @InternalApi
  @ApiOperation(hidden = true, value = "Upsert a webhook event", nickname = "webhookUpsert")
  public ResponseDTO<UpsertWebhookResponseDTO> upsertWebhook(@Valid UpsertWebhookRequestDTO upsertWebhookRequest) {
    final UpsertWebhookResponseDTO upsertWebhookResponse = webhookService.upsertWebhook(upsertWebhookRequest);
    return ResponseDTO.newResponse(upsertWebhookResponse);
  }
}

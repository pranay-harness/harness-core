package io.harness.polling.resource;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.polling.PollingDelegateResponse;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.perpetualtask.PerpetualTaskLogContext;
import io.harness.polling.PollingResponseHandler;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.service.PollingDocument;
import io.harness.polling.service.intfc.PollingService;
import io.harness.security.annotations.InternalApi;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Api("polling")
@Path("polling")
@Produces({"application/json", "text/yaml", "text/html"})
@InternalApi
@ApiOperation(hidden = true, value = "Communication APIs for polling framework.")
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@OwnedBy(HarnessTeam.CDC)
public class PollingResource {
  private KryoSerializer kryoSerializer;
  private PollingResponseHandler pollingResponseHandler;
  private PollingService pollingService;

  @Inject
  public PollingResource(
      KryoSerializer kryoSerializer, PollingResponseHandler pollingResponseHandler, PollingService pollingService) {
    this.kryoSerializer = kryoSerializer;
    this.pollingResponseHandler = pollingResponseHandler;
    this.pollingService = pollingService;
  }

  @POST
  @Path("delegate-response/{perpetualTaskId}")
  public void processPollingResultNg(@PathParam("perpetualTaskId") @NotEmpty String perpetualTaskId,
      @QueryParam("accountId") @NotEmpty String accountId, byte[] serializedExecutionResponse) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new PerpetualTaskLogContext(perpetualTaskId, OVERRIDE_ERROR)) {
      PollingDelegateResponse executionResponse =
          (PollingDelegateResponse) kryoSerializer.asObject(serializedExecutionResponse);
      pollingResponseHandler.handlePollingResponse(perpetualTaskId, accountId, executionResponse);
    }
  }

  @POST
  @Path("subscribe")
  public ResponseDTO<byte[]> subscribe(byte[] pollingItem) {
    PollingItem pollingItem1 = (PollingItem) kryoSerializer.asObject(pollingItem);
    String pollingDocId = pollingService.subscribe(pollingItem1);
    PollingDocument pd = PollingDocument.newBuilder().setPollingDocId(pollingDocId).build();
    return ResponseDTO.newResponse(kryoSerializer.asBytes(pd));
  }

  @POST
  @Path("unsubscribe")
  public Boolean unsubscribe(@Body byte[] pollingItem) {
    PollingItem pollingItem1 = (PollingItem) kryoSerializer.asObject(pollingItem);
    return pollingService.unsubscribe(pollingItem1);
  }
}

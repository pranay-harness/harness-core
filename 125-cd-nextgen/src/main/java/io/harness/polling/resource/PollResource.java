package io.harness.polling.resource;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.polling.PollingDelegateResponse;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskLogContext;
import io.harness.polling.PollingResponseHandler;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/poll")
@Path("/poll")
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CDC)
public class PollResource {
  private KryoSerializer kryoSerializer;
  private PollingResponseHandler pollingResponseHandler;

  @Inject
  public PollResource(KryoSerializer kryoSerializer, PollingResponseHandler pollingResponseHandler) {
    this.kryoSerializer = kryoSerializer;
    this.pollingResponseHandler = pollingResponseHandler;
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
}

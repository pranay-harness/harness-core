package software.wings.resources.secretsmanagement;

import com.google.inject.Inject;

import io.harness.NgManagerServiceDriver;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.TaskId;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("secrets")
@Path("/ng/secrets")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
public class NGSecretsResource {
  private final SecretManager secretManager;
  private final NgManagerServiceDriver ngManagerServiceDriver;

  @Inject
  public NGSecretsResource(SecretManager secretManager, NgManagerServiceDriver ngManagerServiceDriver) {
    this.secretManager = secretManager;
    this.ngManagerServiceDriver = ngManagerServiceDriver;
  }

  @GET
  @Path("{secretId}")
  public RestResponse<EncryptedData> get(@PathParam("secretId") String secretId,
      @QueryParam("accountId") final String accountId, @QueryParam("userId") final String userId) {
    return new RestResponse<>(secretManager.getSecretById(accountId, secretId));
  }

  @GET
  @Path("task")
  public RestResponse<Boolean> sendTaskResponse() {
    SendTaskResultResponse sendTaskResultResponse = ngManagerServiceDriver.sendTaskResult(
        SendTaskResultRequest.newBuilder().setTaskId(TaskId.newBuilder().setId("MangerId").build()).build());
    return new RestResponse<>(sendTaskResultResponse.getAcknowledgement());
  }
}

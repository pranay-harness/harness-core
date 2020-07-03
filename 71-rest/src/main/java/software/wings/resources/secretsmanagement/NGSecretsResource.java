package software.wings.resources.secretsmanagement;

import com.google.inject.Inject;

import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.TaskId;
import io.harness.grpc.ng.manager.DelegateTaskResponseGrpcClient;
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
  private final DelegateTaskResponseGrpcClient delegateTaskResponseGrpcClient;

  @Inject
  public NGSecretsResource(SecretManager secretManager, DelegateTaskResponseGrpcClient delegateTaskResponseGrpcClient) {
    this.secretManager = secretManager;
    this.delegateTaskResponseGrpcClient = delegateTaskResponseGrpcClient;
  }

  @GET
  @Path("{secretId}")
  public RestResponse<EncryptedData> get(@PathParam("secretId") String secretId,
      @QueryParam("accountId") final String accountId, @QueryParam("userId") final String userId) {
    return new RestResponse<>(secretManager.getSecretById(accountId, secretId));
  }

  @GET
  @Path("task")
  public RestResponse<String> sendTaskResponse() {
    SendTaskResultResponse sendTaskResultResponse = delegateTaskResponseGrpcClient.sendTaskResult(
        SendTaskResultRequest.newBuilder().setTaskId(TaskId.newBuilder().setId("MangerId").build()).build());
    return new RestResponse<>(sendTaskResultResponse.getTaskId().getId());
  }
}

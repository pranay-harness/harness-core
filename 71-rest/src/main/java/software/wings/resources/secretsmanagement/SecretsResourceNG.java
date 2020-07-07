package software.wings.resources.secretsmanagement;

import com.google.inject.Inject;

import io.harness.NgManagerServiceDriver;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.delegate.SendTaskResultRequest;
import io.harness.delegate.SendTaskResultResponse;
import io.harness.delegate.TaskId;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.swagger.annotations.Api;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("secrets")
@Path("/ng/secrets")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
public class SecretsResourceNG {
  private final SecretManager secretManager;
  private final NgManagerServiceDriver ngManagerServiceDriver;

  @Inject
  public SecretsResourceNG(SecretManager secretManager, NgManagerServiceDriver ngManagerServiceDriver) {
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
  public RestResponse<PageResponse<EncryptedData>> listSecrets(@QueryParam("accountId") final String accountId,
      @QueryParam("type") final SettingVariableTypes type,
      @DefaultValue("true") @QueryParam("details") boolean details) {
    PageRequest<EncryptedData> pageRequest = new PageRequest<>();
    try {
      pageRequest.addFilter("type", Operator.EQ, type);
      pageRequest.addFilter("accountId", Operator.EQ, accountId);
      return new RestResponse<>(secretManager.listSecretsMappedToAccount(accountId, pageRequest, details));
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }
  }

  // TODO{phoenikx} figure out a way to serialize and deserialize encryptable setting
  @POST
  @Path("encryption-details")
  public RestResponse<List<EncryptedDataDetail>> getEncryptionDetails(@QueryParam("appId") String appId,
      @QueryParam("workflowExecutionId") String workflowExecutionId, EncryptableSetting encryptableSetting) {
    return new RestResponse<>(secretManager.getEncryptionDetails(encryptableSetting, appId, workflowExecutionId));
  }

  @GET
  @Path("task")
  public RestResponse<Boolean> sendTaskResponse() {
    SendTaskResultResponse sendTaskResultResponse = ngManagerServiceDriver.sendTaskResult(
        SendTaskResultRequest.newBuilder().setTaskId(TaskId.newBuilder().setId("MangerId").build()).build());
    return new RestResponse<>(sendTaskResultResponse.getAcknowledgement());
  }
}

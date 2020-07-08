package software.wings.resources.secretsmanagement;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;

import io.harness.NgManagerServiceDriver;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import retrofit2.http.Body;
import software.wings.annotation.EncryptableSetting;
import software.wings.security.annotations.NextGenManagerAuth;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("secrets")
@Path("/ng/secrets")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@Slf4j
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

  @POST
  public RestResponse<String> createSecret(
      @QueryParam("accountId") String accountId, @QueryParam("local") boolean localMode, SecretText secretText) {
    if (localMode) {
      return new RestResponse<>(secretManager.saveSecretUsingLocalMode(accountId, secretText));
    }
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Adding a secret");
      return new RestResponse<>(secretManager.saveSecret(accountId, secretText));
    }
  }

  @PUT
  public RestResponse<Boolean> updateSecret(
      @QueryParam("accountId") String accountId, @QueryParam("uuid") String uuId, @Body SecretText secretText) {
    return new RestResponse<>(secretManager.updateSecret(accountId, uuId, secretText));
  }

  @DELETE
  public RestResponse<Boolean> deleteSecret(
      @QueryParam("accountId") String accountId, @QueryParam("uuid") String uuId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Deleting a secret");
      return new RestResponse<>(secretManager.deleteSecret(accountId, uuId, null));
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
    boolean sendTaskResultResponse = ngManagerServiceDriver.sendTaskResult(generateUuid(), null);
    return new RestResponse<>(sendTaskResultResponse);
  }
}

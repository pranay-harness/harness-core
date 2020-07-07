package io.harness.ng.core.remote;

import com.google.inject.Inject;

import io.harness.ng.core.ResponseDTO;
import io.harness.ng.core.services.api.NGSecretService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("secrets")
@Api("secrets")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class NGSecretResource {
  private final NGSecretService ngSecretService;

  @GET
  @ApiOperation(value = "Get secrets for an account", nickname = "listSecretsForAccount")
  public ResponseDTO<List<EncryptedData>> getSecretsForAccount(
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier,
      @QueryParam("type") @NotNull SettingVariableTypes type,
      @QueryParam("includeDetails") @DefaultValue("true") boolean details) {
    return ResponseDTO.newResponse(ngSecretService.getSecretsByType(accountIdentifier, type, details));
  }

  @GET
  @Path("{secretId}")
  @ApiOperation(value = "Gets a secret by id", nickname = "getSecretById")
  public ResponseDTO<EncryptedData> get(@PathParam("secretId") @NotEmpty String secretId,
      @QueryParam("accountIdentifier") @NotNull String accountIdentifier) {
    EncryptedData encryptedData = ngSecretService.getSecretById(accountIdentifier, secretId);
    return ResponseDTO.newResponse(encryptedData);
  }
}

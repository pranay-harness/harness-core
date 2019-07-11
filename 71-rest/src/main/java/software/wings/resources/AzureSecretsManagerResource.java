package software.wings.resources;

import com.google.inject.Inject;

import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.AzureVaultConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.AzureSecretsManagerServiceImpl;

import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("azure-secrets-manager")
@Path("/azure-secrets-manager")
@Produces("application/json")
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class AzureSecretsManagerResource {
  @Inject AzureSecretsManagerServiceImpl azureSecretsManagerService;

  @POST
  public RestResponse<String> saveAzureSecretsManagerConfig(
      @QueryParam("accountId") final String accountId, @Valid AzureVaultConfig azureVaultConfig) {
    return new RestResponse<>(azureSecretsManagerService.saveAzureSecretsManagerConfig(accountId, azureVaultConfig));
  }

  @DELETE
  public RestResponse<Boolean> deleteAzureVaultConfig(
      @QueryParam("accountId") final String accountId, @QueryParam("configId") final String secretsManagerConfigId) {
    return new RestResponse<>(azureSecretsManagerService.deleteConfig(accountId, secretsManagerConfigId));
  }

  @POST
  @Path("list-vaults")
  public RestResponse<List<String>> listVaults(
      @QueryParam("accountId") final String accountId, AzureVaultConfig azureVaultConfig) {
    return new RestResponse<>(azureSecretsManagerService.listAzureVaults(accountId, azureVaultConfig));
  }
}

package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.VaultConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.security.vault.SecretEngineSummary;
import software.wings.service.intfc.security.VaultService;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rsingh on 10/10/17.
 */
@Api("vault")
@Path("/vault")
@Produces("application/json")
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class VaultResource {
  @Inject private VaultService vaultService;

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> saveVaultConfig(
      @QueryParam("accountId") final String accountId, VaultConfig vaultConfig) {
    return new RestResponse<>(vaultService.saveVaultConfig(accountId, vaultConfig));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteVaultConfig(
      @QueryParam("accountId") final String accountId, @QueryParam("vaultConfigId") final String vaultConfigId) {
    return new RestResponse<>(vaultService.deleteVaultConfig(accountId, vaultConfigId));
  }

  @POST
  @Path("list-engines")
  @Timed
  @ExceptionMetered
  public RestResponse<List<SecretEngineSummary>> listSecretEngines(
      @QueryParam("accountId") final String accountId, VaultConfig vaultConfig) {
    vaultConfig.setAccountId(accountId);
    return new RestResponse<>(vaultService.listSecretEngines(vaultConfig));
  }
}

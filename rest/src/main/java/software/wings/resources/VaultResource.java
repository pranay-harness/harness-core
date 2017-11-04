package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.VaultConfig;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.security.VaultService;

import java.util.Collection;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
@AuthRule(ResourceType.SETTING)
public class VaultResource {
  @Inject private VaultService vaultService;

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> saveVaultConfig(
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

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<Collection<VaultConfig>> lisVaultConfigs(@QueryParam("accountId") final String accountId) {
    return new RestResponse<>(vaultService.listVaultConfigs(accountId));
  }

  @GET
  @Path("/transition-vault")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> transitionVault(@QueryParam("accountId") final String accountId,
      @QueryParam("fromVaultId") String fromVaultId, @QueryParam("toVaultId") String toVaultId) {
    return new RestResponse<>(vaultService.transitionVault(accountId, fromVaultId, toVaultId));
  }
}

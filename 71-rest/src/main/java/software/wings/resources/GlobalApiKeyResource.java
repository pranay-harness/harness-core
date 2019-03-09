package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.GlobalApiKey.ProviderType;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.GlobalApiKeyService;
import software.wings.utils.AccountPermissionUtils;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Api("/global-api-keys")
@Path("/global-api-keys")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.API_KEY)
@AuthRule(permissionType = PermissionType.LOGGED_IN)
/**
 *
 * @author rktummala on 03/07/2019
 */
public class GlobalApiKeyResource {
  private GlobalApiKeyService globalApiKeyService;
  private AccountPermissionUtils accountPermissionUtils;

  @Inject
  public GlobalApiKeyResource(GlobalApiKeyService globalApiKeyService, AccountPermissionUtils accountPermissionUtils) {
    this.globalApiKeyService = globalApiKeyService;
    this.accountPermissionUtils = accountPermissionUtils;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> generate(@NotNull ProviderType providerType) {
    RestResponse response = accountPermissionUtils.checkIfHarnessUser("User not allowed to perform operation");
    if (response == null) {
      response = new RestResponse<>(globalApiKeyService.generate(providerType));
    }
    return response;
  }

  @DELETE
  @Path("{providerType}")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> delete(@NotEmpty @PathParam("providerType") String providerType) {
    RestResponse response = accountPermissionUtils.checkIfHarnessUser("User not allowed to perform operation");
    if (response == null) {
      globalApiKeyService.delete(ProviderType.valueOf(providerType));
      response = new RestResponse<>();
    }
    return response;
  }

  @GET
  @Path("{providerType}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> get(@NotEmpty @PathParam("providerType") String providerType) {
    RestResponse response = accountPermissionUtils.checkIfHarnessUser("User not allowed to perform operation");
    if (response == null) {
      response = new RestResponse<>(globalApiKeyService.get(ProviderType.valueOf(providerType)));
    }
    return response;
  }
}
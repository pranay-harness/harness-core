package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.Base;
import software.wings.beans.RestResponse;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.ApiKeyService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/api-keys")
@Path("/api-keys")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.API_KEY)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class ApiKeyResource {
  private ApiKeyService apiKeyService;

  @Inject
  public ApiKeyResource(ApiKeyService apiKeyService) {
    this.apiKeyService = apiKeyService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> generate(@NotEmpty @QueryParam("accountId") String accountId) {
    return new RestResponse<>(apiKeyService.generate(accountId));
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<ApiKeyEntry>> list(
      @NotEmpty @QueryParam("accountId") String accountId, @BeanParam PageRequest<ApiKeyEntry> pageRequest) {
    pageRequest.addFilter("appId", EQ, Base.GLOBAL_APP_ID);
    pageRequest.addFilter("accountId", EQ, accountId);
    return new RestResponse<>(apiKeyService.list(pageRequest));
  }

  @GET
  @Path("{apiKeyId}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> get(
      @NotEmpty @QueryParam("accountId") String accountId, @NotEmpty @PathParam("apiKeyId") String uuid) {
    return new RestResponse<>(apiKeyService.get(uuid, accountId));
  }

  @DELETE
  @Path("{apiKeyId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> delete(@NotEmpty @PathParam("apiKeyId") String uuid) {
    apiKeyService.delete(uuid);
    return new RestResponse<>();
  }
}
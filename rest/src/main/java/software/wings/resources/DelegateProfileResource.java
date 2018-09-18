package software.wings.resources;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE_SCOPE;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.DelegateProfile;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.DelegateAuth;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.DelegateProfileService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by rishi on 7/31/18
 */
@Api("delegate-profiles")
@Path("/delegate-profiles")
@Produces("application/json")
@Scope(DELEGATE_SCOPE)
@AuthRule(permissionType = LOGGED_IN)
public class DelegateProfileResource {
  private DelegateProfileService delegateProfileService;

  @Inject
  public DelegateProfileResource(DelegateProfileService delegateService) {
    this.delegateProfileService = delegateService;
  }

  @GET
  @ApiImplicitParams(
      { @ApiImplicitParam(name = "accountId", required = true, dataType = "string", paramType = "query") })
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<DelegateProfile>>
  list(@BeanParam PageRequest<DelegateProfile> pageRequest) {
    return new RestResponse<>(delegateProfileService.list(pageRequest));
  }

  @GET
  @Path("{delegateProfileId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfile> get(@PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateProfileService.get(accountId, delegateProfileId));
  }

  @DELETE
  @Path("{delegateProfileId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Void> delete(@PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId) {
    delegateProfileService.delete(accountId, delegateProfileId);
    return new RestResponse<>();
  }

  @DelegateAuth
  @PUT
  @Path("{delegateProfileId}")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateProfile> update(@PathParam("delegateProfileId") @NotEmpty String delegateProfileId,
      @QueryParam("accountId") @NotEmpty String accountId, DelegateProfile delegateProfile) {
    delegateProfile.setAccountId(accountId);
    delegateProfile.setUuid(delegateProfileId);
    return new RestResponse<>(delegateProfileService.update(delegateProfile));
  }

  @POST
  public RestResponse<DelegateProfile> add(
      @QueryParam("accountId") @NotEmpty String accountId, DelegateProfile delegateProfile) {
    delegateProfile.setAccountId(accountId);
    return new RestResponse<>(delegateProfileService.add(delegateProfile));
  }
}

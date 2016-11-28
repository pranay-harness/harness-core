package software.wings.resources;

import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Delegate;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.DelegateService;

import javax.inject.Inject;
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
 * Created by peeyushaggarwal on 11/28/16.
 */
@Api("delegate")
@Path("/delegate")
@Produces("application/json")
public class DelegateResource {
  private DelegateService delegateService;

  @Inject
  public DelegateResource(DelegateService delegateService) {
    this.delegateService = delegateService;
  }

  @GET
  public RestResponse<PageResponse<Delegate>> list(@BeanParam PageRequest<Delegate> pageRequest) {
    return new RestResponse<>(delegateService.list(pageRequest));
  }

  @GET
  @Path("{delegateId}")
  public RestResponse<Delegate> get(
      @PathParam("deletgateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(delegateService.get(accountId, delegateId));
  }

  @DELETE
  @Path("{delegateId}")
  public RestResponse<Void> delete(
      @PathParam("deletgateId") @NotEmpty String delegateId, @QueryParam("accountId") @NotEmpty String accountId) {
    delegateService.delete(accountId, delegateId);
    return new RestResponse<Void>();
  }

  @PUT
  @Path("{delegateId}")
  public RestResponse<Delegate> update(@PathParam("deletgateId") @NotEmpty String delegateId,
      @QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    delegate.setUuid(delegateId);
    return new RestResponse<>(delegateService.update(delegate));
  }

  @POST
  public RestResponse<Delegate> add(@QueryParam("accountId") @NotEmpty String accountId, Delegate delegate) {
    delegate.setAccountId(accountId);
    return new RestResponse<>(delegateService.add(delegate));
  }
}

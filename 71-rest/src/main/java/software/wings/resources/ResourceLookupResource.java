package software.wings.resources;

import static io.harness.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;
import software.wings.service.intfc.ResourceLookupService;

import java.util.List;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * The Class AuditResource.
 */
@Api("resourcelookup")
@Path("/resourcelookup")
public class ResourceLookupResource {
  private ResourceLookupService resourceLookupService;

  @Inject
  public ResourceLookupResource(ResourceLookupService resourceLookupService) {
    this.resourceLookupService = resourceLookupService;
  }

  public void setResourceLookupService(ResourceLookupService resourceLookupService) {
    this.resourceLookupService = resourceLookupService;
  }

  @GET
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<ResourceLookup>> list(
      @QueryParam("accountId") String accountId, @BeanParam PageRequest<ResourceLookup> pageRequest) {
    pageRequest.addFilter(ResourceLookupKeys.accountId, EQ, accountId);
    return new RestResponse<>(resourceLookupService.list(pageRequest));
  }

  @GET
  @Path("acc_resource_types")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listAccResourceTypes(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(resourceLookupService.listAccountLevelResourceTypes());
  }

  @GET
  @Path("app_resource_types")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listAppResourceTypes(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(resourceLookupService.listApplicationLevelResourceTypes());
  }
}
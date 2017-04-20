package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.EntityVersionCollection;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.EntityVersionService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by peeyushaggarwal on 11/2/16.
 */
@Api("versions")
@Path("/versions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PublicApi
public class VersionResource {
  private EntityVersionService entityVersionService;

  @Inject
  public VersionResource(EntityVersionService entityVersionService) {
    this.entityVersionService = entityVersionService;
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<EntityVersionCollection>> list(
      @BeanParam PageRequest<EntityVersionCollection> pageRequest) {
    return new RestResponse<>(entityVersionService.listEntityVersions(pageRequest));
  }
}

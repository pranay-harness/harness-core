package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import software.wings.beans.RestResponse;
import software.wings.beans.ServiceInstance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceInstanceService;

import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by anubhaw on 5/26/16.
 */
@Api("/service-instances")
@Path("service-instances")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceInstanceResource {
  /**
   * The Instance service.
   */
  @Inject ServiceInstanceService instanceService;

  /**
   * List.
   *
   * @param appId       the app id
   * @param envId       the env id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<ServiceInstance>> list(@ApiParam @QueryParam("appId") String appId,
      @ApiParam @QueryParam("envId") String envId, @BeanParam PageRequest<ServiceInstance> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("envId", envId, EQ);
    return new RestResponse<>(instanceService.list(pageRequest));
  }
}

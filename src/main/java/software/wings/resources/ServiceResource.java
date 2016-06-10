package software.wings.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import software.wings.beans.Graph;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ServiceResourceService;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 3/25/16.
 */
@Api("services")
@Path("services")
@Timed
@ExceptionMetered
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ServiceResource {
  private ServiceResourceService serviceResourceService;

  /**
   * Instantiates a new service resource.
   *
   * @param serviceResourceService the service resource service
   */
  @Inject
  public ServiceResource(ServiceResourceService serviceResourceService) {
    this.serviceResourceService = serviceResourceService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Service>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Service> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(serviceResourceService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the rest response
   */
  @GET
  @Path("{serviceId}")
  public RestResponse<Service> get(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    return new RestResponse<>(serviceResourceService.get(appId, serviceId));
  }

  /**
   * Save.
   *
   * @param appId   the app id
   * @param service the service
   * @return the rest response
   */
  @POST
  public RestResponse<Service> save(@QueryParam("appId") String appId, Service service) {
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.save(service));
  }

  /**
   * Update.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param service   the service
   * @return the rest response
   */
  @PUT
  @Path("{serviceId}")
  public RestResponse<Service> update(
      @QueryParam("appId") String appId, @PathParam("serviceId") String serviceId, Service service) {
    service.setUuid(serviceId);
    service.setAppId(appId);
    return new RestResponse<>(serviceResourceService.update(service));
  }

  /**
   * Delete.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @return the rest response
   */
  @DELETE
  @Path("{serviceId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("serviceId") String serviceId) {
    serviceResourceService.delete(appId, serviceId);
    return new RestResponse();
  }

  /**
   * Save command.
   *
   * @param appId     the app id
   * @param serviceId the service id
   * @param command   the command
   * @return the rest response
   */
  @POST
  @Path("{serviceId}/commands")
  public RestResponse<Service> saveCommand(@ApiParam(name = "appId", required = true) @QueryParam("appId") String appId,
      @ApiParam(name = "serviceId", required = true) @PathParam("serviceId") String serviceId,
      @ApiParam(name = "command", required = true,
          examples = @Example(@ExampleProperty("{\"graphName\":\"START\","
              + "\"nodes\":[{\"id\":\"ORIGIN\","
              + "\"type\":\"ORIGIN\",\"x\":0,\"y\":0,"
              + "\"properties\":{}},{\"id\":\"1\","
              + "\"type\":\"EXEC\",\"x\":0,\"y\":0,"
              + "\"properties\":{\"commandPath\""
              + ":\"/home/xxx/tomcat\","
              + "\"commandString\":\"bin/startup.sh\"}}],"
              + "\"links\":[{\"id\":\"linkid\","
              + "\"from\":\"ORIGIN\",\"to\":\"1\","
              + "\"type\":\"ANY\"}]}"))) Graph command) {
    return new RestResponse<>(serviceResourceService.addCommand(appId, serviceId, command));
  }

  /**
   * Delete command.
   *
   * @param appId       the app id
   * @param serviceId   the service id
   * @param commandName the command name
   * @return the rest response
   */
  @DELETE
  @Path("{serviceId}/commands/{commandName}")
  public RestResponse<Service> deleteCommand(@QueryParam("appId") String appId,
      @PathParam("serviceId") String serviceId, @PathParam("commandName") String commandName) {
    return new RestResponse<>(serviceResourceService.deleteCommand(appId, serviceId, commandName));
  }
}

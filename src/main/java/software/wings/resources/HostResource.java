package software.wings.resources;

import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Host;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.HostService;

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

/**
 * Created by anubhaw on 5/9/16.
 */
@Path("/hosts")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class HostResource {
  @Inject private HostService hostService;

  @GET
  public RestResponse<PageResponse<Host>> list(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Host> pageRequest) {
    infraId = hostService.getInfraId(envId, appId);
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("infraId", infraId, EQ);
    return new RestResponse<>(hostService.list(pageRequest));
  }

  @GET
  @Path("{hostId}")
  public RestResponse<Host> get(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("hostId") String hostId) {
    infraId = hostService.getInfraId(envId, appId);
    return new RestResponse<>(hostService.get(appId, infraId, hostId));
  }

  @POST
  public RestResponse save(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, Host baseHost) {
    infraId = hostService.getInfraId(envId, appId);
    baseHost.setAppId(appId);
    baseHost.setInfraId(infraId);
    hostService.bulkSave(baseHost, baseHost.getHostNames());
    return new RestResponse();
  }

  @PUT
  @Path("{hostId}")
  public RestResponse<Host> update(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("hostId") String hostId, Host host) {
    infraId = hostService.getInfraId(envId, appId);
    host.setUuid(hostId);
    host.setInfraId(infraId);
    host.setAppId(appId);
    return new RestResponse<Host>(hostService.update(host));
  }

  @DELETE
  @Path("{hostId}")
  public RestResponse delete(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("hostId") String hostId) {
    infraId = hostService.getInfraId(envId, appId);
    hostService.delete(appId, infraId, hostId);
    return new RestResponse();
  }

  //  @POST
  //  @Path("import-hosts")
  //  @Consumes(MULTIPART_FORM_DATA)
  //  public RestResponse importHosts(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
  //  @QueryParam("envId") String envId,
  //      @FormDataParam("file") InputStream uploadedInputStream,
  //      @FormDataParam("file") FormDataContentDisposition fileDetail,
  //      @FormDataParam("hostAttributes") EnvironmentAttribute attribute) {
  //    infraId = hostService.getInfraId(envId, appId);
  ////    baseHost.setAppId(appId);
  ////    baseHost.setInfraId(infraId);
  ////    hostService.importHosts(baseHost, new BoundedInputStream(uploadedInputStream, 40 * 1000 * 1000)); //TODO: read
  ///from config
  //    return new RestResponse();
  //  }
  //
  //  @GET
  //  @Path("export-hosts")
  //  @Encoded
  //  public Response exportHosts(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
  //  @QueryParam("envId") String envId) {
  //    infraId = hostService.getInfraId(envId, appId);
  //    File hostsFile = hostService.exportHosts(appId, infraId);
  //    Response.ResponseBuilder response = Response.ok(hostsFile, MediaType.TEXT_PLAIN);
  //    response.header("Content-Disposition", "attachment; filename=" + hostsFile.getName());
  //    return response.build();
  //  }
}

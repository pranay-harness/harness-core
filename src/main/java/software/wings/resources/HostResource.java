package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.Host;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.InputStream;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/9/16.
 */
@Api("hosts")
@Path("/hosts")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class HostResource {
  private HostService hostService;
  private InfraService infraService;

  /**
   * Instantiates a new Host resource.
   *
   * @param hostService  the host service
   * @param infraService the infra service
   */
  @Inject
  public HostResource(HostService hostService, InfraService infraService) {
    this.hostService = hostService;
    this.infraService = infraService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param infraId     the infra id
   * @param envId       the env id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<Host>> list(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @BeanParam PageRequest<Host> pageRequest) {
    infraId = infraService.getInfraIdByEnvId(appId, envId);
    pageRequest.addFilter("appId", appId, EQ);
    pageRequest.addFilter("infraId", infraId, EQ);
    return new RestResponse<>(hostService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @param envId   the env id
   * @param hostId  the host id
   * @return the rest response
   */
  @GET
  @Path("{hostId}")
  public RestResponse<Host> get(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("hostId") String hostId) {
    infraId = infraService.getInfraIdByEnvId(appId, envId);
    return new RestResponse<>(hostService.get(appId, infraId, hostId));
  }

  /**
   * Save.
   *
   * @param appId    the app id
   * @param infraId  the infra id
   * @param envId    the env id
   * @param baseHost the base host
   * @return the rest response
   */
  @POST
  public RestResponse save(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, Host baseHost) {
    infraId = infraService.getInfraIdByEnvId(appId, envId);
    baseHost.setAppId(appId);
    baseHost.setInfraId(infraId);
    hostService.bulkSave(envId, baseHost);
    return new RestResponse();
  }

  /**
   * Update.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @param envId   the env id
   * @param hostId  the host id
   * @param host    the host
   * @return the rest response
   */
  @PUT
  @Path("{hostId}")
  public RestResponse<Host> update(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("hostId") String hostId, Host host) {
    infraId = infraService.getInfraIdByEnvId(appId, envId);
    host.setUuid(hostId);
    host.setInfraId(infraId);
    host.setAppId(appId);
    return new RestResponse<Host>(hostService.update(host));
  }

  /**
   * Delete.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @param envId   the env id
   * @param hostId  the host id
   * @return the rest response
   */
  @DELETE
  @Path("{hostId}")
  public RestResponse delete(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("hostId") String hostId) {
    infraId = infraService.getInfraIdByEnvId(appId, envId);
    hostService.delete(appId, infraId, hostId);
    return new RestResponse();
  }

  /**
   * Import hosts.
   *
   * @param appId               the app id
   * @param infraId             the infra id
   * @param envId               the env id
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @return the rest response
   */
  @POST
  @Path("import")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse importHosts(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    infraId = infraService.getInfraIdByEnvId(appId, envId);
    hostService.importHosts(
        appId, infraId, new BoundedInputStream(uploadedInputStream, 40 * 1000 * 1000)); // TODO: read from config
    return new RestResponse();
  }

  /**
   * Export hosts.
   *
   * @param appId   the app id
   * @param infraId the infra id
   * @param envId   the env id
   * @return the response
   */
  @GET
  @Path("export")
  @Encoded
  public Response exportHosts(
      @QueryParam("appId") String appId, @QueryParam("infraId") String infraId, @QueryParam("envId") String envId) {
    infraId = infraService.getInfraIdByEnvId(appId, envId);
    File hostsFile = hostService.exportHosts(appId, infraId);
    Response.ResponseBuilder response = Response.ok(hostsFile, MediaType.TEXT_PLAIN);
    response.header("Content-Disposition", "attachment; filename=" + hostsFile.getName());
    return response.build();
  }
}

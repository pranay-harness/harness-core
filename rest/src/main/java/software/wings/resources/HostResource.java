package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.app.MainConfiguration;
import software.wings.beans.RestResponse;
import software.wings.beans.infrastructure.Host;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.HostService;
import software.wings.utils.BoundedInputStream;

import java.io.InputStream;
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
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 5/9/16.
 */
@Api("hosts")
@Path("/hosts")
@Produces("application/json")
@Consumes("application/json")
@Scope(APPLICATION)
@AuthRule(permissionType = ENV)
public class HostResource {
  private HostService hostService;
  private MainConfiguration configuration;

  /**
   * Instantiates a new Host resource.
   *
   * @param hostService   the host service
   * @param configuration the configuration
   */
  @Inject
  public HostResource(HostService hostService, MainConfiguration configuration) {
    this.hostService = hostService;
    this.configuration = configuration;
  }

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Host>> list(@BeanParam PageRequest<Host> pageRequest) {
    return new RestResponse<>(hostService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   * @return the rest response
   */
  @GET
  @Path("{hostId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Host> get(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @PathParam("hostId") String hostId) {
    return new RestResponse<>(hostService.get(appId, envId, hostId));
  }

  /**
   * Update.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   * @param host   the host
   * @return the rest response
   */
  @PUT
  @Path("{hostId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Host> update(@QueryParam("appId") String appId, @QueryParam("envId") String envId,
      @PathParam("hostId") String hostId, Host host) {
    host.setUuid(hostId);
    host.setAppId(appId);
    return new RestResponse<>(hostService.update(envId, host));
  }

  /**
   * Delete.
   *
   * @param appId  the app id
   * @param envId  the env id
   * @param hostId the host id
   * @return the rest response
   */
  @DELETE
  @Path("{hostId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = Action.UPDATE)
  public RestResponse delete(
      @QueryParam("appId") String appId, @QueryParam("envId") String envId, @PathParam("hostId") String hostId) {
    hostService.delete(appId, envId, hostId);
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
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = Action.UPDATE)
  public RestResponse importHosts(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    hostService.importHosts(infraId, appId, envId,
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getHostUploadLimit()));
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
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ENV, action = Action.UPDATE)
  public Response exportHosts(
      @QueryParam("appId") String appId, @QueryParam("infraId") String infraId, @QueryParam("envId") String envId) {
    //    File hostsFile = hostService.exportHosts(appId);
    //    Response.ResponseBuilder response = Response.ok(hostsFile, MediaType.TEXT_PLAIN);
    //    response.header("Content-Disposition", "attachment; filename=" + hostsFile.getName());
    //    return response.build();
    return Response.ok().build();
  }
}

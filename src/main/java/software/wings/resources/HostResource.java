package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ArtifactSource.SourceType.HTTP;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.beans.Host;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.HostService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.File;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
  public RestResponse<Host> save(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, Host host) {
    infraId = hostService.getInfraId(envId, appId);
    host.setAppId(appId);
    host.setInfraId(infraId);
    return new RestResponse<Host>(hostService.save(host));
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

  @POST
  @Path("import/{fileType}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse importHosts(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("fileType") HostFileType fileType,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    infraId = hostService.getInfraId(envId, appId);
    if (sourceType.equals(HTTP)) {
      uploadedInputStream =
          BoundedInputStream.getBoundedStreamForUrl(urlString, 40 * 1000 * 1000); // TODO: read from config
    }
    hostService.importHosts(appId, infraId, uploadedInputStream, fileType);
    return new RestResponse();
  }

  @GET
  @Path("export/{fileType}")
  @Encoded
  public Response exportHosts(@QueryParam("appId") String appId, @QueryParam("infraId") String infraId,
      @QueryParam("envId") String envId, @PathParam("fileType") HostFileType fileType) {
    infraId = hostService.getInfraId(envId, appId);
    File hostsFile = hostService.exportHosts(appId, infraId, fileType);
    Response.ResponseBuilder response = Response.ok(hostsFile, MediaType.TEXT_PLAIN);
    response.header("Content-Disposition", "attachment; filename=" + hostsFile.getName());
    return response.build();
  }
}

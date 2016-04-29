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
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.InfraService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostFileHelper.HostFileType;

import java.io.File;
import java.io.InputStream;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/infra")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfraResource {
  @Inject private InfraService infraService;

  @GET
  @Path("envId")
  public RestResponse<PageResponse<Infra>> listInfra(
      @PathParam("envId") String envId, @BeanParam PageRequest<Infra> pageRequest) {
    pageRequest.addFilter("envId", envId, EQ);
    return new RestResponse<>(infraService.list(envId, pageRequest));
  }

  @POST
  @Path("envId")
  public RestResponse<Infra> createInfra(@PathParam("envId") String envId, Infra infra) {
    return new RestResponse<>(infraService.save(infra, envId));
  }

  @GET
  @Path("/{infraId}/hosts")
  public RestResponse<PageResponse<Host>> listHosts(
      @PathParam("infraId") String infraId, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter("infraId", infraId, EQ);
    return new RestResponse<>(infraService.listHosts(pageRequest));
  }

  @GET
  @Path("{infraId}/hosts/{hostId}")
  public RestResponse<Host> listHosts(@PathParam("infraId") String infraId, @PathParam("hostId") String hostId) {
    return new RestResponse<>(infraService.getHost(infraId, hostId));
  }

  @POST
  @Path("{infraId}/hosts")
  public RestResponse<Host> createHost(@PathParam("infraId") String infraId, Host host) {
    return new RestResponse<Host>(infraService.createHost(infraId, host));
  }

  @PUT
  @Path("{infraId}/hosts/{hostId}")
  public RestResponse<Host> updateHost(
      @PathParam("infraId") String infraId, @PathParam("hostId") String hostId, Host host) {
    host.setUuid(hostId);
    return new RestResponse<Host>(infraService.updateHost(infraId, host));
  }

  @PUT
  @Path("hosts/{hostId}/tags/{tagId}")
  public RestResponse<Host> applyTag(@PathParam("hostId") String hostId, @PathParam("tagId") String tagId) {
    return new RestResponse<>(infraService.tagHost(hostId, tagId));
  }

  @POST
  @Path("{infraId}/hosts/import/{fileType}")
  @Consumes(MULTIPART_FORM_DATA)
  public void importHosts(@PathParam("infraId") String infraId, @PathParam("fileType") HostFileType fileType,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    if (sourceType.equals(HTTP)) {
      uploadedInputStream =
          BoundedInputStream.getBoundedStreamForUrl(urlString, 40 * 1000 * 1000); // TODO: read from config
    }
    infraService.importHosts(infraId, uploadedInputStream, fileType);
  }

  @GET
  @Path("{infraId}/hosts/export/{fileType}")
  @Encoded
  public Response exportHosts(@PathParam("infraId") String infraId, @PathParam("fileType") HostFileType fileType) {
    File hostsFile = infraService.exportHosts(infraId, fileType);
    Response.ResponseBuilder response = Response.ok(hostsFile, MediaType.TEXT_PLAIN);
    response.header("Content-Disposition", "attachment; filename=" + hostsFile.getName());
    return response.build();
  }
}

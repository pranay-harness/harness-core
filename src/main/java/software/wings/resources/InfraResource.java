package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.*;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.InfraService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostFileHelper.HostFileType;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.InputStream;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ArtifactSource.SourceType.HTTP;

@Path("/infra")
@AuthRule
@Timed
@ExceptionMetered
@Produces("application/json")
@Consumes("application/json")
public class InfraResource {
  @Inject private InfraService infraService;

  @GET
  @Path("envID")
  public RestResponse<PageResponse<Infra>> listInfra(
      @PathParam("envID") String envID, @BeanParam PageRequest<Infra> pageRequest) {
    pageRequest.addFilter("envID", envID, SearchFilter.OP.EQ);
    return new RestResponse<>(infraService.listInfra(envID, pageRequest));
  }

  @POST
  @Path("envID")
  public RestResponse<Infra> createInfra(@PathParam("envID") String envID, Infra infra) {
    return new RestResponse<>(infraService.createInfra(infra, envID));
  }

  @GET
  @Path("/{infraID}/hosts")
  public RestResponse<PageResponse<Host>> listHosts(
      @PathParam("infraID") String infraID, @BeanParam PageRequest<Host> pageRequest) {
    pageRequest.addFilter("infraID", infraID, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Host>>(infraService.listHosts(pageRequest));
  }

  @GET
  @Path("{infraID}/hosts/{hostID}")
  public RestResponse<Host> listHosts(@PathParam("infraID") String infraID, @PathParam("hostID") String hostID) {
    return new RestResponse<>(infraService.getHost(infraID, hostID));
  }

  @POST
  @Path("{infraID}/hosts")
  public RestResponse<Host> createHost(@PathParam("infraID") String infraID, Host host) {
    return new RestResponse<Host>(infraService.createHost(infraID, host));
  }

  @PUT
  @Path("{infraID}/hosts")
  public RestResponse<Host> updateHost(@PathParam("infraID") String infraID, Host host) {
    return new RestResponse<Host>(infraService.updateHost(infraID, host));
  }

  @POST
  @Path("tags/{envID}")
  public RestResponse<Tag> saveTag(@PathParam("envID") String envID, Tag tag) {
    return new RestResponse<>(infraService.createTag(envID, tag));
  }

  @PUT
  @Path("hosts/{hostID}/tag/{tagID}")
  public RestResponse<Host> applyTag(@PathParam("hostID") String hostID, @PathParam("tagID") String tagID) {
    return new RestResponse<>(infraService.applyTag(hostID, tagID));
  }

  @POST
  @Path("{infraID}/hosts/import/{fileType}")
  @Consumes(MULTIPART_FORM_DATA)
  public void importHosts(@PathParam("infraID") String infraID, @PathParam("fileType") HostFileType fileType,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    if (sourceType.equals(HTTP)) {
      uploadedInputStream =
          BoundedInputStream.getBoundedStreamForURL(urlString, 40 * 1000 * 1000); // TODO: read from config
    }
    infraService.importHosts(infraID, uploadedInputStream, fileType);
  }

  @GET
  @Path("{infraID}/hosts/export/{fileType}")
  @Encoded
  public Response exportHosts(@PathParam("infraID") String infraID, @PathParam("fileType") HostFileType fileType) {
    File hostsFile = infraService.exportHosts(infraID, fileType);
    Response.ResponseBuilder response = Response.ok(hostsFile, MediaType.TEXT_PLAIN);
    response.header("Content-Disposition", "attachment; filename=" + hostsFile.getName());
    return response.build();
  }
}

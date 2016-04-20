package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Artifact;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.service.intfc.ArtifactService;
import software.wings.utils.Validator;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * ArtifactResource.
 *
 * @author Rishi
 */
@Path("/artifacts")
public class ArtifactResource {
  private ArtifactService artifactService;

  @Inject
  public ArtifactResource(ArtifactService artifactService) {
    this.artifactService = artifactService;
  }

  @GET
  @Path("{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<Artifact>> list(
      @PathParam("applicationId") String applicationId, @BeanParam PageRequest<Artifact> pageRequest) {
    pageRequest.addFilter("application", applicationId, SearchFilter.OP.EQ);
    return new RestResponse<PageResponse<Artifact>>(artifactService.list(pageRequest));
  }

  @GET
  @Path("{applicationId}/{artifactId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Artifact> get(
      @PathParam("applicationId") String applicationId, @PathParam("artifactId") String artifactId) {
    return new RestResponse<Artifact>(artifactService.get(applicationId, artifactId));
  }

  @POST
  @Path("{applicationId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Artifact> save(@PathParam("applicationId") String applicationId, Artifact artifact) {
    Validator.equalCheck(applicationId, artifact.getApplication().getUuid());
    return new RestResponse<Artifact>(artifactService.create(artifact));
  }

  @PUT
  @Path("{applicationId}/{artifactId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Artifact> update(Artifact artifact) {
    return new RestResponse<Artifact>(artifactService.update(artifact));
  }

  /*
    @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_JSON)
        @Path("upload")
        public RestResponse<String> uploadArtifact(
                @FormDataParam("part") InputStream uploadedInputStream,
                @FormDataParam("part") FormDataContentDisposition fileDetail) {

  //		logger.debug("uploadedInputStream :" + uploadedInputStream);
  //		logger.debug("fileDetail :" + fileDetail);
  //
  //		String filename = fileDetail.getFileName();
  //		logger.debug("filename Received :" + filename);
  //
  //		// save it
  //		String uploadedFilename = dumpFile(uploadedInputStream);
  //		logger.debug("File uploaded to : " + uploadedFilename);
  //
  //		return new RestResponse<String>(uploadedFilename);
                return new RestResponse<String>(null);
        }*/

  @GET
  @Path("download/{applicationId}/{artifactId}")
  @Encoded
  public Response download(@PathParam("applicationId") String applicationId, @PathParam("artifactId") String artifactId)
      throws IOException, GeneralSecurityException {
    File artifactFile = artifactService.download(applicationId, artifactId);
    ResponseBuilder response = Response.ok(artifactFile, MediaType.APPLICATION_OCTET_STREAM);
    response.header("Content-Disposition", "attachment; filename=" + artifactFile.getName());
    return response.build();
  }
}

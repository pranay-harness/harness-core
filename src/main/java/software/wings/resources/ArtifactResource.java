package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import software.wings.beans.Artifact;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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
import javax.ws.rs.QueryParam;
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
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<PageResponse<Artifact>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Artifact> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    return new RestResponse<>(artifactService.list(pageRequest));
  }

  @GET
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Artifact> get(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId) {
    return new RestResponse<>(artifactService.get(appId, artifactId));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Artifact> save(@QueryParam("appId") String appId, Artifact artifact) {
    artifact.setAppId(appId);
    return new RestResponse<>(artifactService.create(artifact));
  }

  @PUT
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  @Produces("application/json")
  public RestResponse<Artifact> update(
      @QueryParam("appId") String appId, @PathParam("artifactId") String artifactId, Artifact artifact) {
    artifact.setUuid(artifactId);
    artifact.setAppId(appId);
    return new RestResponse<>(artifactService.update(artifact));
  }

  @GET
  @Path("{artifactId}/artifactFile/{serviceId}")
  @Encoded
  public Response download(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId,
      @PathParam("serviceId") String serviceId) throws IOException, GeneralSecurityException {
    File artifactFile = artifactService.download(appId, artifactId, serviceId);
    ResponseBuilder response = Response.ok(artifactFile, MediaType.APPLICATION_OCTET_STREAM);
    response.header("Content-Disposition", "attachment; filename=" + artifactFile.getName());
    return response.build();
  }
}

package software.wings.resources;

import static java.util.stream.Collectors.toList;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.SortOrder.OrderType.DESC;
import static software.wings.dl.PageResponse.Builder.*;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactService;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
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
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * ArtifactResource.
 *
 * @author Rishi
 */
@Api("artifacts")
@Path("/artifacts")
@Produces("application/json")
@AuthRule(ResourceType.APPLICATION)
public class ArtifactResource {
  private ArtifactService artifactService;

  /**
   * Instantiates a new artifact resource.
   *
   * @param artifactService the artifact service
   */
  @Inject
  public ArtifactResource(ArtifactService artifactService) {
    this.artifactService = artifactService;
  }

  /**
   * List.
   *
   * @param appId       the app id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<Artifact>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<Artifact> pageRequest) {
    pageRequest.addFilter("appId", appId, SearchFilter.Operator.EQ);
    Comparator<Artifact> byBuildNo = Comparator.comparing(artifact -> artifact.getBuildNo(), Comparator.reverseOrder());
    PageResponse<Artifact> pageResponse = artifactService.list(pageRequest, false);
    if (pageResponse != null && pageResponse.getResponse() != null) {
      List<Artifact> response = pageResponse.getResponse().stream().sorted(byBuildNo).collect(toList());
      return new RestResponse<>(aPageResponse().withTotal(pageResponse.getTotal()).withResponse(response).build());
    }
    return new RestResponse<>(pageResponse);
  }

  /**
   * Gets the.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the rest response
   */
  @GET
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Artifact> get(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId) {
    return new RestResponse<>(artifactService.get(appId, artifactId, true));
  }

  /**
   * Save.
   *
   * @param appId    the app id
   * @param artifact the artifact
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<Artifact> save(@QueryParam("appId") String appId, Artifact artifact) {
    artifact.setAppId(appId);
    return new RestResponse<>(artifactService.create(artifact));
  }

  /**
   * Update.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @param artifact   the artifact
   * @return the rest response
   */
  @PUT
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  public RestResponse<Artifact> update(
      @QueryParam("appId") String appId, @PathParam("artifactId") String artifactId, Artifact artifact) {
    artifact.setUuid(artifactId);
    artifact.setAppId(appId);
    return new RestResponse<>(artifactService.update(artifact));
  }

  /**
   * Update.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the rest response
   */
  @DELETE
  @Path("{artifactId}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId) {
    artifactService.delete(appId, artifactId);
    return new RestResponse();
  }

  /**
   * Download.
   *
   * @param appId      the app id
   * @param artifactId the artifact id
   * @return the response
   * @throws IOException              Signals that an I/O exception has occurred.
   * @throws GeneralSecurityException the general security exception
   */
  @GET
  @Path("{artifactId}/artifactFile")
  @Encoded
  @Timed
  @ExceptionMetered
  public Response download(@QueryParam("appId") String appId, @PathParam("artifactId") String artifactId)
      throws IOException, GeneralSecurityException {
    File artifactFile = artifactService.download(appId, artifactId);
    ResponseBuilder response = Response.ok(artifactFile, MediaType.APPLICATION_OCTET_STREAM);
    response.header("Content-Disposition", "attachment; filename=" + artifactFile.getName());
    return response.build();
  }
}

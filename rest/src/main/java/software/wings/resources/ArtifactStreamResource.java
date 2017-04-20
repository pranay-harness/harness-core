package software.wings.resources;

import static software.wings.beans.RestResponse.Builder.aRestResponse;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import net.redhogs.cronparser.CronExpressionDescriptor;
import net.redhogs.cronparser.DescriptionTypeEnum;
import net.redhogs.cronparser.I18nMessages;
import net.redhogs.cronparser.Options;
import software.wings.beans.ErrorCode;
import software.wings.beans.RestResponse;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAction;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.stencils.Stencil;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * ArtifactStreamResource class.
 *
 * @author Rishi
 */
@Api("artifactstreams")
@Path("/artifactstreams")
@Produces("application/json")
@Consumes("application/json")
@AuthRule(ResourceType.APPLICATION)
public class ArtifactStreamResource {
  private ArtifactStreamService artifactStreamService;

  private AppService appService;

  /**
   * Instantiates a new Artifact stream resource.
   *
   * @param artifactStreamService the artifact stream service
   * @param appService            the app service
   */
  @Inject
  public ArtifactStreamResource(ArtifactStreamService artifactStreamService, AppService appService) {
    this.appService = appService;
    this.artifactStreamService = artifactStreamService;
  }

  /**
   * Sets artifact stream service.
   *
   * @param artifactStreamService the artifact stream service
   */
  public void setArtifactStreamService(ArtifactStreamService artifactStreamService) {
    this.artifactStreamService = artifactStreamService;
  }

  /**
   * Sets app service.
   *
   * @param appService the app service
   */
  public void setAppService(AppService appService) {
    this.appService = appService;
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
  public RestResponse<PageResponse<ArtifactStream>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<ArtifactStream> pageRequest) {
    pageRequest.addFilter("appId", appId, EQ);
    return new RestResponse<>(artifactStreamService.list(pageRequest));
  }

  /**
   * Gets the.
   *
   * @param appId    the app id
   * @param streamId the stream id
   * @return the rest response
   */
  @GET
  @Path("{streamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> get(@QueryParam("appId") String appId, @PathParam("streamId") String streamId) {
    return new RestResponse<>(artifactStreamService.get(appId, streamId));
  }

  /**
   * Save rest response.
   *
   * @param appId          the app id
   * @param artifactStream the artifact stream
   * @return the rest response
   */
  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> save(@QueryParam("appId") String appId, ArtifactStream artifactStream) {
    try {
      if (!appService.exist(appId)) {
        throw new NotFoundException("application with id " + appId + " not found.");
      }
      artifactStream.setAppId(appId);
      return new RestResponse<>(artifactStreamService.create(artifactStream));
    } catch (Exception exception) {
      exception.printStackTrace();
      throw exception;
    }
  }

  /**
   * Update rest response.
   *
   * @param appId          the app id
   * @param streamId       the stream id
   * @param artifactStream the artifact stream
   * @return the rest response
   */
  @PUT
  @Path("{streamId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> update(
      @QueryParam("appId") String appId, @PathParam("streamId") String streamId, ArtifactStream artifactStream) {
    artifactStream.setUuid(streamId);
    artifactStream.setAppId(appId);
    return new RestResponse<>(artifactStreamService.update(artifactStream));
  }

  /**
   * Delete.
   *
   * @param appId the app id
   * @param id    the id
   * @return the rest response
   */
  @DELETE
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("id") String id) {
    artifactStreamService.delete(appId, id);
    return new RestResponse<>();
  }

  /**
   * Add action rest response.
   *
   * @param appId                the app id
   * @param streamId             the stream id
   * @param artifactStreamAction the artifact stream action
   * @return the rest response
   */
  @POST
  @Path("{streamId}/actions")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> addAction(@QueryParam("appId") String appId,
      @PathParam("streamId") String streamId, ArtifactStreamAction artifactStreamAction) {
    return new RestResponse<>(artifactStreamService.addStreamAction(appId, streamId, artifactStreamAction));
  }

  /**
   * Update action rest response.
   *
   * @param appId                the app id
   * @param streamId             the stream id
   * @param workflowId           the workflow id
   * @param artifactStreamAction the artifact stream action
   * @return the rest response
   */
  @PUT
  @Path("{streamId}/actions/{workflowId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> updateAction(@QueryParam("appId") String appId,
      @PathParam("streamId") String streamId, @PathParam("workflowId") String workflowId,
      ArtifactStreamAction artifactStreamAction) {
    artifactStreamAction.setWorkflowId(workflowId);
    return new RestResponse<>(artifactStreamService.updateStreamAction(appId, streamId, artifactStreamAction));
  }

  /**
   * Add action rest response.
   *
   * @param appId      the app id
   * @param streamId   the stream id
   * @param workflowId the action id
   * @return the rest response
   */
  @DELETE
  @Path("{streamId}/actions/{workflowId}")
  @Timed
  @ExceptionMetered
  public RestResponse<ArtifactStream> deleteAction(@QueryParam("appId") String appId,
      @PathParam("streamId") String streamId, @PathParam("workflowId") String workflowId) {
    return new RestResponse<>(artifactStreamService.deleteStreamAction(appId, streamId, workflowId));
  }

  /**
   * Translate cron rest response.
   *
   * @param inputMap the input map
   * @return the rest response
   */
  @POST
  @Path("cron/translate")
  @Timed
  @ExceptionMetered
  public RestResponse<String> translateCron(Map<String, String> inputMap) {
    try {
      return new RestResponse<>(CronExpressionDescriptor.getDescription(
          DescriptionTypeEnum.FULL, inputMap.get("expression"), new Options(), I18nMessages.DEFAULT_LOCALE));
    } catch (ParseException e) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Incorrect cron expression");
    }
  }

  @GET
  @Path("stencils")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Stencil>> installedPluginSettingSchema(
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId) {
    return aRestResponse().withResource(artifactStreamService.getArtifactStreamSchema(appId, serviceId)).build();
  }

  @GET
  @Path("buildsource-types")
  @Timed
  @ExceptionMetered
  public RestResponse<Map<String, String>> getBuildSourceTypes(
      @QueryParam("appId") String appId, @QueryParam("serviceId") String serviceId) {
    return new RestResponse<>(artifactStreamService.getSupportedBuildSourceTypes(appId, serviceId));
  }
}

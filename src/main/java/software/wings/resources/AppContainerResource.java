package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.artifact.ArtifactSource.SourceType.HTTP;
import static software.wings.beans.FileUrlSource.Builder.aFileUrlSource;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.app.MainConfiguration;
import software.wings.beans.AppContainer;
import software.wings.beans.artifact.ArtifactSource.SourceType;
import software.wings.beans.FileUploadSource;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppContainerService;
import software.wings.utils.BoundedInputStream;

import java.io.BufferedInputStream;
import java.io.InputStream;
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
 * Created by anubhaw on 5/4/16.
 */
@Api("app-containers")
@Path("/app-containers")
@Produces("application/json")
@Timed
@ExceptionMetered
public class AppContainerResource {
  @Inject private AppContainerService appContainerService;
  @Inject private MainConfiguration configuration;

  /**
   * List.
   *
   * @param appId   the app id
   * @param request the request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<AppContainer>> list(
      @QueryParam("appId") String appId, @BeanParam PageRequest<AppContainer> request) {
    request.addFilter("appId", appId, EQ);
    return new RestResponse<>(appContainerService.list(request));
  }

  /**
   * Gets the.
   *
   * @param appId          the app id
   * @param appContainerId the app container id
   * @return the rest response
   */
  @GET
  @Path("{appContainerId}")
  public RestResponse<AppContainer> get(
      @QueryParam("appId") String appId, @PathParam("appContainerId") String appContainerId) {
    return new RestResponse<>(appContainerService.get(appId, appContainerId));
  }

  /**
   * Upload platform.
   *
   * @param appId               the app id
   * @param sourceType          the source type
   * @param urlString           the url string
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param appContainer        the app container
   * @return the rest response
   */
  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<AppContainer> uploadPlatform(@QueryParam("appId") String appId,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(appId);
    appContainer.setFileName(fileDetail.getFileName());
    setSourceForAppContainer(sourceType, urlString, appContainer);

    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, appContainer.getSource().getSourceType());

    return new RestResponse<>(appContainerService.save(appContainer, uploadedInputStream, PLATFORMS));
  }

  /**
   * Update platform.
   *
   * @param appId               the app id
   * @param appContainerId      the app container id
   * @param sourceType          the source type
   * @param urlString           the url string
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param appContainer        the app container
   * @return the rest response
   */
  @PUT
  @Path("{appContainerId}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<AppContainer> updatePlatform(@QueryParam("appId") String appId,
      @PathParam("appContainerId") String appContainerId, @FormDataParam("sourceType") SourceType sourceType,
      @FormDataParam("url") String urlString, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(appId);
    appContainer.setUuid(appContainerId);
    appContainer.setFileName(fileDetail.getFileName());
    setSourceForAppContainer(sourceType, urlString, appContainer);
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, appContainer.getSource().getSourceType());

    return new RestResponse<>(appContainerService.update(appContainer, uploadedInputStream, PLATFORMS));
  }

  private void setSourceForAppContainer(
      SourceType sourceType, String urlString, AppContainer appContainer) { // Fixme: use jsonSubType
    if (sourceType.equals(HTTP)) {
      appContainer.setSource(aFileUrlSource().withUrl(urlString).build());
    } else {
      appContainer.setSource(new FileUploadSource());
    }
  }

  /**
   * Delete platform.
   *
   * @param appId          the app id
   * @param appContainerId the app container id
   * @return the rest response
   */
  @DELETE
  @Path("{appContainerId}")
  public RestResponse deletePlatform(
      @QueryParam("appId") String appId, @PathParam("appContainerId") String appContainerId) {
    appContainerService.delete(appId, appContainerId);
    return new RestResponse();
  }

  private InputStream updateTheUploadedInputStream(String urlString, InputStream inputStream, SourceType sourceType) {
    return sourceType.equals(HTTP) ? new BufferedInputStream(BoundedInputStream.getBoundedStreamForUrl(
                                         urlString, configuration.getFileUploadLimits().getAppContainerLimit()))
                                   : new BufferedInputStream(new BoundedInputStream(
                                         inputStream, configuration.getFileUploadLimits().getAppContainerLimit()));
  }
}

package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.app.MainConfiguration;
import software.wings.beans.AppContainer;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppContainerService;
import software.wings.utils.BoundedInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
   * @param appId     the app id
   * @param accountId the account id
   * @param request   the request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<AppContainer>> list(@QueryParam("appId") @DefaultValue(GLOBAL_APP_ID) String appId,
      @QueryParam("accountId") @NotEmpty String accountId, @BeanParam PageRequest<AppContainer> request) {
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
  public RestResponse<AppContainer> get(@QueryParam("appId") @DefaultValue(GLOBAL_APP_ID) String appId,
      @PathParam("appContainerId") String appContainerId) {
    return new RestResponse<>(appContainerService.get(appId, appContainerId));
  }

  /**
   * Upload platform.
   *
   * @param accountId           the account id
   * @param appId               the app id
   * @param urlString           the url string
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param appContainer        the app container
   * @return the rest response
   */
  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<AppContainer> uploadPlatform(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("appId") @DefaultValue(GLOBAL_APP_ID) String appId, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(appId);
    appContainer.setFileName(fileDetail.getFileName());
    appContainer.setAccountId(accountId);

    uploadedInputStream = new BufferedInputStream(
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getAppContainerLimit()));
    return new RestResponse<>(appContainerService.save(appContainer, uploadedInputStream, PLATFORMS));
  }

  /**
   * Update platform.
   *
   * @param accountId           the account id
   * @param appId               the app id
   * @param appContainerId      the app container id
   * @param urlString           the url string
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param appContainer        the app container
   * @return the rest response
   */
  @PUT
  @Path("{appContainerId}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<AppContainer> updatePlatform(@QueryParam("accountId") @NotEmpty String accountId,
      @QueryParam("appId") String appId, @PathParam("appContainerId") String appContainerId,
      @FormDataParam("url") String urlString, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(appId);
    appContainer.setUuid(appContainerId);
    appContainer.setFileName(fileDetail.getFileName());
    appContainer.setAccountId(accountId);
    uploadedInputStream = new BufferedInputStream(
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getAppContainerLimit()));

    return new RestResponse<>(appContainerService.update(appContainer, uploadedInputStream, PLATFORMS));
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

  /**
   * Download response.
   *
   * @param accountId      the account id
   * @param appContainerId the app container id
   * @return the response
   */
  @GET
  @Path("{appContainerId}/download")
  @Encoded
  public Response download(
      @QueryParam("accountId") String accountId, @PathParam("appContainerId") String appContainerId) {
    File appContainerFile = appContainerService.download(accountId, appContainerId);
    Response.ResponseBuilder response = Response.ok(appContainerFile, "application/x-unknown");
    response.header("Content-Disposition", "attachment; filename=" + appContainerFile.getName());
    return response.build();
  }
}

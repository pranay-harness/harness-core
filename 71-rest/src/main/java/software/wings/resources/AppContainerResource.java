package software.wings.resources;

import static io.harness.delegate.service.DelegateAgentFileService.FileBucket.PLATFORMS;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.security.PermissionAttribute.ResourceType.APP_STACK;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import io.harness.stream.BoundedInputStream;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.app.MainConfiguration;
import software.wings.beans.AppContainer;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AppContainerService;

import java.io.BufferedInputStream;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Created by anubhaw on 5/4/16.
 */
@Api("app-containers")
@Path("/app-containers")
@Produces("application/json")
@Scope(APP_STACK)
public class AppContainerResource {
  @Inject private AppContainerService appContainerService;
  @Inject private MainConfiguration configuration;

  /**
   * List.
   *
   * @param accountId the account id
   * @param request   the request
   * @return the rest response
   */
  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<PageResponse<AppContainer>> list(
      @QueryParam("accountId") @NotEmpty String accountId, @BeanParam PageRequest<AppContainer> request) {
    return new RestResponse<>(appContainerService.list(request));
  }

  /**
   * Gets the.
   *
   * @param accountId      the account id
   * @param appContainerId the app container id
   * @return the rest response
   */
  @GET
  @Path("{appContainerId}")
  @Timed
  @ExceptionMetered
  public RestResponse<AppContainer> get(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("appContainerId") String appContainerId) {
    return new RestResponse<>(appContainerService.get(accountId, appContainerId));
  }

  /**
   * Upload platform.
   *
   * @param accountId           the account id
   * @param urlString           the url string
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param appContainer        the app container
   * @return the rest response
   */
  @POST
  @Consumes(MULTIPART_FORM_DATA)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<AppContainer> uploadPlatform(@QueryParam("accountId") @NotEmpty String accountId,
      @FormDataParam("url") String urlString, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(GLOBAL_APP_ID);
    appContainer.setAccountId(accountId);
    appContainer.setFileName(new File(fileDetail.getFileName()).getName());

    uploadedInputStream = new BufferedInputStream(
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getAppContainerLimit()));
    return new RestResponse<>(appContainerService.save(appContainer, uploadedInputStream, PLATFORMS));
  }

  /**
   * Update platform.
   *
   * @param accountId           the account id
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
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse<AppContainer> updatePlatform(@QueryParam("accountId") @NotEmpty String accountId,
      @PathParam("appContainerId") String appContainerId, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam AppContainer appContainer) {
    appContainer.setAppId(GLOBAL_APP_ID);
    appContainer.setUuid(appContainerId);
    appContainer.setFileName(new File(fileDetail.getFileName()).getName());
    appContainer.setAccountId(accountId);
    uploadedInputStream = new BufferedInputStream(
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getAppContainerLimit()));
    return new RestResponse<>(appContainerService.update(appContainer, uploadedInputStream, PLATFORMS));
  }

  /**
   * Delete platform.
   *
   * @param appContainerId the app container id
   * @return the rest response
   */
  @DELETE
  @Path("{appContainerId}")
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public RestResponse deletePlatform(
      @QueryParam("accountId") @NotEmpty String accountId, @PathParam("appContainerId") String appContainerId) {
    appContainerService.delete(accountId, appContainerId);
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
  @Timed
  @ExceptionMetered
  public Response download(
      @QueryParam("accountId") String accountId, @PathParam("appContainerId") String appContainerId) {
    File appContainerFile = appContainerService.download(accountId, appContainerId);
    ResponseBuilder response = Response.ok(appContainerFile, "application/x-unknown");
    response.header("Content-Disposition", "attachment; filename=" + appContainerFile.getName());
    return response.build();
  }
}

package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ArtifactSource.SourceType.HTTP;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppContainer;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.beans.FileUploadSource;
import software.wings.beans.FileUrlSource;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppContainerService;
import software.wings.utils.BoundedInputStream;

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

@Path("/app-containers")
@Produces("application/json")
@AuthRule
@Timed
@ExceptionMetered
public class AppContainerResource {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private AppContainerService appContainerService;

  @GET
  public RestResponse<PageResponse<AppContainer>> list(
      @QueryParam("app_id") String appId, @BeanParam PageRequest<AppContainer> request) {
    request.addFilter("appId", appId, EQ);
    return new RestResponse<>(appContainerService.list(request));
  }

  @GET
  @Path("{app_container_id}")
  public RestResponse<AppContainer> get(@PathParam("app_container_id") String appContainerId) {
    return new RestResponse<>(appContainerService.get(appContainerId));
  }

  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> uploadPlatform(@QueryParam("app_id") String appId,
      @FormDataParam("standard") boolean standard, @FormDataParam("name") String name,
      @FormDataParam("version") String version, @FormDataParam("description") String description,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("md5") String md5,
      @FormDataParam("url") String urlString, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    AppContainer appContainer =
        createPlatformSoftwareFromRequest(appId, name, version, md5, description, urlString, standard, sourceType,
            uploadedInputStream); // TODO: Encapsulate FormDataParam into one object
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, appContainer.getSource().getSourceType());
    String fileId = appContainerService.save(appContainer, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  @PUT
  @Path("app-containers/{app_container_id}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> updatePlatform(@QueryParam("app_id") String appId,
      @PathParam("app_container_id") String appContainerId, @FormDataParam("standard") boolean standard,
      @FormDataParam("name") String name, @FormDataParam("version") String version,
      @FormDataParam("description") String description, @FormDataParam("sourceType") SourceType sourceType,
      @FormDataParam("md5") String md5, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    AppContainer appContainer =
        createPlatformSoftwareFromRequest(appId, name, version, md5, description, urlString, standard, sourceType,
            uploadedInputStream); // TODO: Encapsulate FormDataParam into one object
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, appContainer.getSource().getSourceType());
    String fileId = appContainerService.update(appContainerId, appContainer, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  @DELETE
  @Path("{app_container_id}")
  public void deletePlatform(@PathParam("app_id") String appId, @PathParam("app_container_id") String appContainerId) {
    appContainerService.delete(appContainerId);
  }

  private InputStream updateTheUploadedInputStream(String urlString, InputStream inputStream, SourceType sourceType) {
    if (sourceType.equals(HTTP)) {
      inputStream =
          BoundedInputStream.getBoundedStreamForUrl(urlString, 4 * 1000 * 1000 * 1000); // TODO: read from config
    }
    return inputStream;
  }

  private AppContainer createPlatformSoftwareFromRequest(String appId, String name, String version, String md5,
      String description, String urlString, boolean standard, SourceType sourceType, InputStream inputStream) {
    AppContainer appContainer = new AppContainer(name, md5);
    appContainer.setAppId(appId);
    appContainer.setStandard(standard);
    appContainer.setDescription(description);
    if (sourceType.equals(HTTP)) {
      FileUrlSource fileUrlSource = new FileUrlSource();
      fileUrlSource.setUrl(urlString);
      appContainer.setSource(fileUrlSource);
    } else {
      appContainer.setSource(new FileUploadSource());
    }
    return appContainer;
  }
}

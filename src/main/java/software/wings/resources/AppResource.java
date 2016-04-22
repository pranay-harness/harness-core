package software.wings.resources;

import static com.google.common.collect.ImmutableMap.of;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ArtifactSource.SourceType.HTTP;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ArtifactSource.SourceType;
import software.wings.beans.FileUploadSource;
import software.wings.beans.FileUrlSource;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.beans.PlatformSoftware;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.utils.BoundedInputStream;

import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * Application Resource class.
 *
 * @author Rishi
 */
@Path("/apps")
@AuthRule
@Produces("application/json")
@Timed
@ExceptionMetered
public class AppResource {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private AppService appService;

  @Inject
  public AppResource(AppService appService) {
    this.appService = appService;
  }

  @GET
  public RestResponse<PageResponse<Application>> list(@BeanParam PageRequest<Application> pageRequest) {
    return new RestResponse<>(appService.list(pageRequest));
  }

  @POST
  public RestResponse<Application> save(Application app) {
    return new RestResponse<>(appService.save(app));
  }

  @PUT
  public RestResponse<Application> update(Application app) {
    return new RestResponse<>(appService.update(app));
  }

  @GET
  @Path("{appId}")
  public RestResponse<Application> get(@PathParam("appId") String appId) {
    return new RestResponse<>(appService.findByUuid(appId));
  }

  @DELETE
  @Path("{appId}")
  public RestResponse delete(@PathParam("appId") String appId) {
    appService.deleteApp(appId);
    return new RestResponse(of("status", "success"));
  }

  @POST
  @Path("{appId}/platforms")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> uploadPlatform(@PathParam("appId") String appId,
      @FormDataParam("standard") boolean standard, @FormDataParam("fileName") String fileName,
      @FormDataParam("version") String version, @FormDataParam("description") String description,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("md5") String md5,
      @FormDataParam("url") String urlString, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    PlatformSoftware platformSoftware =
        createPlatformSoftwareFromRequest(appId, fileName, version, md5, description, urlString, standard, sourceType,
            uploadedInputStream); // TODO: Encapsulate FormDataParam into one object
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, platformSoftware.getSource().getSourceType());
    String fileId = appService.savePlatformSoftware(platformSoftware, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  @PUT
  @Path("{appId}/platforms/{platformId}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> updatePlatform(@PathParam("appId") String appId,
      @PathParam("platformId") String platformId, @FormDataParam("standard") boolean standard,
      @FormDataParam("fileName") String fileName, @FormDataParam("version") String version,
      @FormDataParam("description") String description, @FormDataParam("sourceType") SourceType sourceType,
      @FormDataParam("md5") String md5, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    PlatformSoftware platformSoftware =
        createPlatformSoftwareFromRequest(appId, fileName, version, md5, description, urlString, standard, sourceType,
            uploadedInputStream); // TODO: Encapsulate FormDataParam into one object
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, platformSoftware.getSource().getSourceType());
    String fileId = appService.updatePlatformSoftware(platformId, platformSoftware, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  @GET
  @Path("{appId}/platforms")
  public RestResponse<List<PlatformSoftware>> fetchPlatforms(@PathParam("appId") String appId) {
    return new RestResponse<>(appService.getPlatforms(appId));
  }

  @GET
  @Path("{appId}/platforms/{platformId}")
  public RestResponse<PlatformSoftware> fetchPlatform(
      @PathParam("appId") String appId, @PathParam("platformId") String platformId) {
    return new RestResponse<>(appService.getPlatform(appId, platformId));
  }

  @DELETE
  @Path("{appId}/platforms/{platformId}")
  public void deletePlatform(@PathParam("appId") String appId, @PathParam("platformId") String platformId) {
    appService.deletePlatform(appId, platformId);
  }

  private InputStream updateTheUploadedInputStream(String urlString, InputStream inputStream, SourceType sourceType) {
    if (sourceType.equals(HTTP)) {
      inputStream =
          BoundedInputStream.getBoundedStreamForURL(urlString, 4 * 1000 * 1000 * 1000); // TODO: read from config
    }
    return inputStream;
  }

  private PlatformSoftware createPlatformSoftwareFromRequest(String appId, String fileName, String version, String md5,
      String description, String urlString, boolean standard, SourceType sourceType, InputStream inputStream) {
    PlatformSoftware platformSoftware = new PlatformSoftware(fileName, md5);
    platformSoftware.setAppID(appId);
    platformSoftware.setStandard(standard);
    platformSoftware.setDescription(description);
    if (sourceType.equals(HTTP)) {
      FileUrlSource fileUrlSource = new FileUrlSource();
      fileUrlSource.setUrl(urlString);
      platformSoftware.setSource(fileUrlSource);
    } else {
      platformSoftware.setSource(new FileUploadSource());
    }
    return platformSoftware;
  }
}

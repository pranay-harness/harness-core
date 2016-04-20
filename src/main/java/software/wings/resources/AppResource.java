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
 * Application Resource class
 *
 * @author Rishi
 */
@Path("/apps")
@AuthRule
@Produces("application/json")
@Timed
@ExceptionMetered
public class AppResource {
  private static final Logger logger = LoggerFactory.getLogger(AppResource.class);

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
  @Path("{appID}")
  public RestResponse<Application> get(@PathParam("appID") String appID) {
    return new RestResponse<>(appService.findByUUID(appID));
  }

  @DELETE
  @Path("{appID}")
  public RestResponse delete(@PathParam("appID") String appID) {
    appService.deleteApp(appID);
    return new RestResponse(of("status", "success"));
  }

  @POST
  @Path("{appID}/platforms")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> uploadPlatform(@PathParam("appID") String appID,
      @FormDataParam("standard") boolean standard, @FormDataParam("fileName") String fileName,
      @FormDataParam("version") String version, @FormDataParam("description") String description,
      @FormDataParam("sourceType") SourceType sourceType, @FormDataParam("md5") String md5,
      @FormDataParam("url") String urlString, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    PlatformSoftware platformSoftware =
        createPlatformSoftwareFromRequest(appID, fileName, version, md5, description, urlString, standard, sourceType,
            uploadedInputStream); // TODO: Encapsulate FormDataParam into one object
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, platformSoftware.getSource().getSourceType());
    String fileId = appService.savePlatformSoftware(platformSoftware, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  @PUT
  @Path("{appID}/platforms/{platformID}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> updatePlatform(@PathParam("appID") String appID,
      @PathParam("platformID") String platformID, @FormDataParam("standard") boolean standard,
      @FormDataParam("fileName") String fileName, @FormDataParam("version") String version,
      @FormDataParam("description") String description, @FormDataParam("sourceType") SourceType sourceType,
      @FormDataParam("md5") String md5, @FormDataParam("url") String urlString,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    PlatformSoftware platformSoftware =
        createPlatformSoftwareFromRequest(appID, fileName, version, md5, description, urlString, standard, sourceType,
            uploadedInputStream); // TODO: Encapsulate FormDataParam into one object
    uploadedInputStream =
        updateTheUploadedInputStream(urlString, uploadedInputStream, platformSoftware.getSource().getSourceType());
    String fileId = appService.updatePlatformSoftware(platformID, platformSoftware, uploadedInputStream, PLATFORMS);
    return new RestResponse<>(fileId);
  }

  @GET
  @Path("{appID}/platforms")
  public RestResponse<List<PlatformSoftware>> fetchPlatforms(@PathParam("appID") String appID) {
    return new RestResponse<>(appService.getPlatforms(appID));
  }

  @GET
  @Path("{appID}/platforms/{platformID}")
  public RestResponse<PlatformSoftware> fetchPlatform(
      @PathParam("appID") String appID, @PathParam("platformID") String platformID) {
    return new RestResponse<>(appService.getPlatform(appID, platformID));
  }

  @DELETE
  @Path("{appID}/platforms/{platformID}")
  public void deletePlatform(@PathParam("appID") String appID, @PathParam("platformID") String platformID) {
    appService.deletePlatform(appID, platformID);
  }

  private InputStream updateTheUploadedInputStream(String urlString, InputStream inputStream, SourceType sourceType) {
    if (sourceType.equals(HTTP)) {
      inputStream =
          BoundedInputStream.getBoundedStreamForURL(urlString, 4 * 1000 * 1000 * 1000); // TODO: read from config
    }
    return inputStream;
  }

  private PlatformSoftware createPlatformSoftwareFromRequest(String appID, String fileName, String version, String md5,
      String description, String urlString, boolean standard, SourceType sourceType, InputStream inputStream) {
    PlatformSoftware platformSoftware = new PlatformSoftware(fileName, md5);
    platformSoftware.setAppID(appID);
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

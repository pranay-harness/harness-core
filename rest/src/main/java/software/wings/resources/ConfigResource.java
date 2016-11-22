package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityType.SERVICE;

import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.app.MainConfiguration;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.EntityVersion;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ConfigService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.JsonUtils;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
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
 * Application Resource class.
 *
 * @author Rishi
 */
@Api("configs")
@Path("/configs")
@Produces("application/json")
public class ConfigResource {
  @Inject private ConfigService configService;
  @Inject private MainConfiguration configuration;

  /**
   * List.
   *
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<ConfigFile>> list(@BeanParam PageRequest<ConfigFile> pageRequest) {
    return new RestResponse<>(configService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param appId               the app id
   * @param entityId            the entity id
   * @param entityType          the entity type
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param configFile          the config file
   * @return the rest response
   */
  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> save(@QueryParam("appId") String appId, @QueryParam("entityId") String entityId,
      @QueryParam("entityType") EntityType entityType, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam ConfigFile configFile) {
    configFile.setAppId(appId);
    configFile.setEntityId(entityId);
    configFile.setEntityType(entityType == null ? SERVICE : entityType);
    configFile.setFileName(fileDetail.getFileName());
    try {
      Map<String, EntityVersion> envIdVersionMap =
          JsonUtils.asObject(configFile.getEnvIdVersionMapString(), new TypeReference<Map<String, EntityVersion>>() {});
      configFile.setEnvIdVersionMap(envIdVersionMap);
    } catch (Exception e) {
      // Ignore
    }
    String fileId = configService.save(configFile,
        new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getConfigFileLimit()));
    return new RestResponse<>(fileId);
  }

  /**
   * Gets the.
   *
   * @param appId    the app id
   * @param configId the config id
   * @return the rest response
   */
  @GET
  @Path("{configId}")
  public RestResponse<ConfigFile> get(@QueryParam("appId") String appId, @PathParam("configId") String configId) {
    return new RestResponse<>(configService.get(appId, configId, false));
  }

  /**
   * Update.
   *
   * @param appId               the app id
   * @param configId            the config id
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param configFile          the config file
   * @return the rest response
   */
  @PUT
  @Path("{configId}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse update(@QueryParam("appId") String appId, @PathParam("configId") String configId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam ConfigFile configFile) {
    configFile.setAppId(appId);
    configFile.setUuid(configId);
    try {
      Map<String, EntityVersion> envIdVersionMap =
          JsonUtils.asObject(configFile.getEnvIdVersionMapString(), new TypeReference<Map<String, EntityVersion>>() {});
      configFile.setEnvIdVersionMap(envIdVersionMap);
    } catch (Exception e) {
      // Ignore
    }
    if (fileDetail != null && fileDetail.getFileName() != null) {
      configFile.setFileName(fileDetail.getFileName());
    }
    configService.update(configFile,
        uploadedInputStream == null
            ? uploadedInputStream
            : new BoundedInputStream(uploadedInputStream, configuration.getFileUploadLimits().getConfigFileLimit()));
    return new RestResponse();
  }

  /**
   * Delete.
   *
   * @param appId    the app id
   * @param configId the config id
   * @return the rest response
   */
  @DELETE
  @Path("{configId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("configId") String configId) {
    configService.delete(appId, configId);
    return new RestResponse();
  }

  /**
   * Export logs response.
   *
   * @param appId    the app id
   * @param configId the config id
   * @return the response
   */
  @GET
  @Path("{configId}/download")
  @Encoded
  public Response downloadConfig(@QueryParam("appId") String appId, @PathParam("configId") String configId,
      @QueryParam("version") Integer version) {
    File configFile = configService.download(appId, configId, version);
    Response.ResponseBuilder response = Response.ok(configFile, "application/x-unknown");
    response.header("Content-Disposition", "attachment; filename=" + configFile.getName());
    return response.build();
  }

  /**
   * Delete by entity rest response.
   *
   * @param appId      the app id
   * @param templateId the template id
   * @param entityId   the entity id
   * @return the rest response
   */
  @DELETE
  @Path("/entity/{entityId}")
  public RestResponse deleteByEntity(@QueryParam("appId") String appId,
      @DefaultValue(DEFAULT_TEMPLATE_ID) @QueryParam("templateId") String templateId,
      @PathParam("entityId") String entityId) {
    configService.deleteByEntityId(appId, templateId, entityId);
    return new RestResponse();
  }
}

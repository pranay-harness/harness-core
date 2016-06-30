package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.ConfigFile;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ConfigService;

import java.io.InputStream;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

// TODO: Auto-generated Javadoc

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

  /**
   * List.
   *
   * @param entityId    the entity id
   * @param templateId  the template id
   * @param pageRequest the page request
   * @return the rest response
   */
  @GET
  public RestResponse<PageResponse<ConfigFile>> list(@QueryParam("appId") String appId,
      @QueryParam("entityId") String entityId,
      @DefaultValue(DEFAULT_TEMPLATE_ID) @QueryParam("templateId") String templateId,
      @BeanParam PageRequest<ConfigFile> pageRequest) {
    pageRequest.addFilter("templateId", templateId, EQ);
    pageRequest.addFilter("entityId", entityId, EQ);
    return new RestResponse<>(configService.list(pageRequest));
  }

  /**
   * Save.
   *
   * @param entityId            the entity id
   * @param uploadedInputStream the uploaded input stream
   * @param fileDetail          the file detail
   * @param configFile          the config file
   * @return the rest response
   */
  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> save(@QueryParam("appId") String appId, @QueryParam("entityId") String entityId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam ConfigFile configFile) {
    configFile.setEntityId(entityId);
    configFile.setAppId(appId);
    String fileId = configService.save(configFile, uploadedInputStream);
    return new RestResponse<>(fileId);
  }

  /**
   * Gets the.
   *
   * @param configId the config id
   * @return the rest response
   */
  @GET
  @Path("{configId}")
  public RestResponse<ConfigFile> get(@QueryParam("appId") String appId, @PathParam("configId") String configId) {
    return new RestResponse<>(configService.get(configId));
  }

  /**
   * Update.
   *
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
    configFile.setUuid(configId);
    configService.update(configFile, uploadedInputStream);
    return new RestResponse();
  }

  /**
   * Delete.
   *
   * @param configId the config id
   * @return the rest response
   */
  @DELETE
  @Path("{configId}")
  public RestResponse delete(@QueryParam("appId") String appId, @PathParam("configId") String configId) {
    configService.delete(configId);
    return new RestResponse();
  }
}

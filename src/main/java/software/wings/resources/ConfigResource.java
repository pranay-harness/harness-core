package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import com.google.inject.Inject;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ConfigFile;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.ConfigService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Application Resource class.
 *
 * @author Rishi
 */

@Path("/configs/{entityId}")
public class ConfigResource {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private ConfigService configService;

  @GET
  public RestResponse<List<ConfigFile>> fetchConfigs(@PathParam("entityId") String entityId) {
    return new RestResponse<>(configService.list(entityId));
  }

  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> uploadConfig(@PathParam("entityId") String entityId,
      @FormDataParam("fileName") String fileName, @FormDataParam("relativePath") String relativePath,
      @FormDataParam("md5") String md5, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    ConfigFile configFile = new ConfigFile(entityId, fileName, relativePath, md5);
    String fileId = configService.save(configFile, uploadedInputStream);
    return new RestResponse<>(fileId);
  }

  @GET
  @Path("{configId}")
  public RestResponse<ConfigFile> fetchConfig(@PathParam("configId") String configId) {
    return new RestResponse<>(configService.get(configId));
  }

  @PUT
  @Path("{configId}")
  @Consumes(MULTIPART_FORM_DATA)
  public void updateConfig(@PathParam("entityId") String entityId, @PathParam("configId") String configId,
      @FormDataParam("fileName") String fileName, @FormDataParam("relativePath") String relativePath,
      @FormDataParam("md5") String md5, @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    ConfigFile configFile = new ConfigFile(entityId, fileName, relativePath, md5);
    configFile.setUuid(configId);
    configService.update(configFile, uploadedInputStream);
  }

  @DELETE
  @Path("{configId}")
  public void delete(@PathParam("configId") String configId) {
    configService.delete(configId);
  }

  @GET
  @Path("download/{applicationId}")
  @Encoded
  public Response download(@PathParam("applicationId") String applicationId)
      throws IOException, GeneralSecurityException {
    try {
      URL url = this.getClass().getResource("/temp-config.txt");
      ResponseBuilder response = Response.ok(new File(url.toURI()), MediaType.APPLICATION_OCTET_STREAM);
      response.header("Content-Disposition", "attachment; filename=app.config");
      return response.build();
    } catch (URISyntaxException ex) {
      return Response.noContent().build();
    }
  }
}

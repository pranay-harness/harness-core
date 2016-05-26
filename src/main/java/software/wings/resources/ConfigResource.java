package software.wings.resources;

import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import software.wings.beans.ConfigFile;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ConfigService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Application Resource class.
 *
 * @author Rishi
 */

@Path("/configs")
@Produces("application/json")
public class ConfigResource {
  @Inject private ConfigService configService;

  @GET
  public RestResponse<PageResponse<ConfigFile>> list(@QueryParam("entityId") String entityId,
      @DefaultValue(DEFAULT_TEMPLATE_ID) @QueryParam("templateId") String templateId,
      @BeanParam PageRequest<ConfigFile> pageRequest) {
    pageRequest.addFilter("templateId", templateId, EQ);
    pageRequest.addFilter("entityId", entityId, EQ);
    return new RestResponse<>(configService.list(pageRequest));
  }

  @POST
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse<String> save(@QueryParam("entityId") String entityId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam ConfigFile configFile) {
    configFile.setEntityId(entityId);
    String fileId = configService.save(configFile, uploadedInputStream);
    return new RestResponse<>(fileId);
  }

  @GET
  @Path("{configId}")
  public RestResponse<ConfigFile> get(@PathParam("configId") String configId) {
    return new RestResponse<>(configService.get(configId));
  }

  @PUT
  @Path("{configId}")
  @Consumes(MULTIPART_FORM_DATA)
  public RestResponse update(@PathParam("configId") String configId,
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @BeanParam ConfigFile configFile) {
    configService.update(configFile, uploadedInputStream);
    return new RestResponse();
  }

  @DELETE
  @Path("{configId}")
  public RestResponse delete(@PathParam("configId") String configId) {
    configService.delete(configId);
    return new RestResponse();
  }

  @GET
  @Path("download/{appId}")
  @Encoded
  public Response download(@PathParam("appId") String appId) throws IOException, GeneralSecurityException {
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

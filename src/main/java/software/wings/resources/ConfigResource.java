package software.wings.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

/**
 * Application Resource class
 *
 * @author Rishi
 */
@Path("/configs")
public class ConfigResource {
  private final Logger logger = LoggerFactory.getLogger(getClass());

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

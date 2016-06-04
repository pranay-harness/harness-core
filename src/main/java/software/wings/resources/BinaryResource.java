package software.wings.resources;

import io.swagger.annotations.Api;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

// TODO: Auto-generated Javadoc

/**
 * The Class BinaryResource.
 */
@Api("bins")
@Path("/bins")
public class BinaryResource {
  /**
   * Framework.
   *
   * @return the response
   * @throws IOException              Signals that an I/O exception has occurred.
   * @throws GeneralSecurityException the general security exception
   */
  @GET
  @Path("framework")
  @Encoded
  public Response framework() throws IOException, GeneralSecurityException {
    return downloadFileFromResource("wings_main.pl");
  }

  private Response downloadFileFromResource(String filename) {
    try {
      URL url = this.getClass().getResource("/" + filename);
      ResponseBuilder response = Response.ok(new File(url.toURI()), MediaType.APPLICATION_OCTET_STREAM);
      response.header("Content-Disposition", "attachment; filename=" + filename);
      return response.build();
    } catch (URISyntaxException ex) {
      return Response.noContent().build();
    }
  }

  /**
   * Sample.
   *
   * @return the response
   * @throws IOException              Signals that an I/O exception has occurred.
   * @throws GeneralSecurityException the general security exception
   */
  @GET
  @Path("sample")
  @Encoded
  public Response sample() throws IOException, GeneralSecurityException {
    return downloadFileFromResource("sample.tar.gz");
  }
}

package software.wings.resources;

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

@Path("/bins")
public class BinaryResource {
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
    } catch (URISyntaxException e) {
      return Response.noContent().build();
    }
  }

  @GET
  @Path("sample")
  @Encoded
  public Response sample() throws IOException, GeneralSecurityException {
    return downloadFileFromResource("sample.tar.gz");
  }
}

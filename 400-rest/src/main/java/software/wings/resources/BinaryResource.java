/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
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
  @Timed
  @ExceptionMetered
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
}

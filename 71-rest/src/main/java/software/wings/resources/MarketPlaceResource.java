package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.MarketPlaceService;

import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Api("mktplace")
@Path("/mktplace")
@Produces(MediaType.APPLICATION_JSON)
public class MarketPlaceResource {
  @Inject MarketPlaceService marketPlaceService;

  @Path("aws-signup")
  @POST
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @PublicApi
  @Timed
  @ExceptionMetered
  public javax.ws.rs.core.Response awsMarketLogin(@FormParam(value = "x-amzn-marketplace-token") String token,
      @Context HttpServletRequest request, @Context HttpServletResponse response) throws URISyntaxException {
    return marketPlaceService.processAWSMarktPlaceOrder(token);
  }
}

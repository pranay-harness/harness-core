/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessModule._940_MARKETPLACE_INTEGRATIONS;
import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.marketplace.gcp.GcpMarketPlaceApiHandler;
import io.harness.security.annotations.PublicApi;

import software.wings.service.intfc.AwsMarketPlaceApiHandler;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
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
import javax.ws.rs.core.Response;

@Api("mktplace")
@Path("/mktplace")
@OwnedBy(GTM)
@TargetModule(_940_MARKETPLACE_INTEGRATIONS)
@Produces(MediaType.APPLICATION_JSON)
public class MarketPlaceResource {
  @Inject private AwsMarketPlaceApiHandler awsMarketPlaceApiHandler;
  @Inject private GcpMarketPlaceApiHandler gcpMarketPlaceApiHandler;

  @Path("aws-signup")
  @POST
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @PublicApi
  @Timed
  @ExceptionMetered
  public javax.ws.rs.core.Response awsMarketLogin(@FormParam(value = "x-amzn-marketplace-token") String token,
      @Context HttpServletRequest request, @Context HttpServletResponse response) throws URISyntaxException {
    return awsMarketPlaceApiHandler.processAWSMarktPlaceOrder(token);
  }

  @POST
  @Path("/gcp-signup")
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @PublicApi
  public Response gcpSignUp(@FormParam(value = "x-gcp-marketplace-token") String token,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    return gcpMarketPlaceApiHandler.signUp(token);
  }

  @POST
  @Path("/gcp-billing")
  @Produces(MediaType.TEXT_HTML)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @PublicApi
  public Response gcpBilling(@FormParam(value = "x-gcp-marketplace-token") String token,
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    return gcpMarketPlaceApiHandler.registerBillingOnlyTransaction(token);
  }
}

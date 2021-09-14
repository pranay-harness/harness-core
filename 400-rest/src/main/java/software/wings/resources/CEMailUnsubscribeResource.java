/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import io.harness.ccm.communication.CECommunicationsService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Api("ceMailUnsubscribe")
@Path("/ceMailUnsubscribe")
@Produces("application/json")
public class CEMailUnsubscribeResource {
  private final CECommunicationsService communicationsService;

  @Inject
  public CEMailUnsubscribeResource(CECommunicationsService communicationsService) {
    this.communicationsService = communicationsService;
  }

  @POST
  @Path("{id}")
  @PublicApi
  public RestResponse unsubscribe(@PathParam("id") String id) {
    communicationsService.unsubscribe(id);
    return new RestResponse<>("You have been successfully unsubscribed.");
  }
}

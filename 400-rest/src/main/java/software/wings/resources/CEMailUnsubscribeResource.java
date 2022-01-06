/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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

/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.ccm.config.GcpOrganization;
import io.harness.ccm.config.GcpOrganizationService;
import io.harness.rest.RestResponse;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("gcp-organizations")
@Path("/gcp-organizations")
@Produces("application/json")
public class GcpOrganizationResource {
  private GcpOrganizationService gcpOrganizationService;
  @Inject
  public GcpOrganizationResource(GcpOrganizationService gcpOrganizationService) {
    this.gcpOrganizationService = gcpOrganizationService;
  }

  @POST
  @Path("validate-serviceaccount")
  @Timed
  @ExceptionMetered
  public RestResponse validatePermission(@QueryParam("accountId") String accountId, GcpOrganization gcpOrganization) {
    return new RestResponse<>(gcpOrganizationService.validate(gcpOrganization));
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<GcpOrganization> save(
      @QueryParam("accountId") String accountId, GcpOrganization gcpOrganization) {
    gcpOrganization.setAccountId(accountId);
    return new RestResponse<>(gcpOrganizationService.upsert(gcpOrganization));
  }

  @DELETE
  @Path("{uuid}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@NotEmpty @QueryParam("accountId") String accountId, @PathParam("uuid") String uuid) {
    gcpOrganizationService.delete(accountId, uuid);
    return new RestResponse();
  }
}

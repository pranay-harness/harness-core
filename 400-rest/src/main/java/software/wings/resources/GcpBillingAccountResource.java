/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountService;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.rest.RestResponse;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Slf4j
@Api("billing-accounts")
@Path("/billing-accounts")
@Produces("application/json")
public class GcpBillingAccountResource {
  private final GcpBillingAccountService gcpBillingAccountService;
  private final AWSAccountService awsAccountService;

  @Inject
  public GcpBillingAccountResource(
      GcpBillingAccountService gcpBillingAccountService, AWSAccountService awsAccountService) {
    this.gcpBillingAccountService = gcpBillingAccountService;
    this.awsAccountService = awsAccountService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<GcpBillingAccount> save(
      @QueryParam("accountId") String accountId, GcpBillingAccount billingAccount) {
    billingAccount.setAccountId(accountId);
    GcpBillingAccount gcpBillingAccount;
    gcpBillingAccount = gcpBillingAccountService.create(billingAccount);
    return new RestResponse<>(gcpBillingAccount);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/verify-account")
  public RestResponse verifyAccess(
      @QueryParam("accountId") String accountId, @QueryParam("settingId") String settingId) {
    awsAccountService.updateAccountPermission(accountId, settingId);
    return new RestResponse();
  }

  @GET
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse<GcpBillingAccount> get(
      @QueryParam("accountId") String accountId, @PathParam("id") String billingAccountId) {
    return new RestResponse<>(gcpBillingAccountService.get(billingAccountId));
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<GcpBillingAccount>> list(@NotEmpty @QueryParam("accountId") String accountId,
      @QueryParam("organizationSettingId") String organizationSettingId) {
    return new RestResponse<>(gcpBillingAccountService.list(accountId, organizationSettingId));
  }

  @PUT
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse update(@PathParam("id") String billingAccountId, GcpBillingAccount billingAccount) {
    gcpBillingAccountService.update(billingAccountId, billingAccount);
    return new RestResponse();
  }

  @DELETE
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(
      @NotEmpty @QueryParam("accountId") String accountId, @PathParam("id") String billingAccountId) {
    GcpBillingAccount gcpBillingAccount = gcpBillingAccountService.get(billingAccountId);
    gcpBillingAccountService.delete(
        gcpBillingAccount.getAccountId(), gcpBillingAccount.getOrganizationSettingId(), billingAccountId);
    return new RestResponse();
  }
}

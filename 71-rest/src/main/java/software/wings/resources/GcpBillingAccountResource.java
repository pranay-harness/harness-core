package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpBillingAccountService;
import io.harness.ccm.setup.service.intfc.AWSAccountService;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

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
  public RestResponse<String> save(@QueryParam("accountId") String accountId, GcpBillingAccount billingAccount) {
    billingAccount.setAccountId(accountId);
    return new RestResponse<>(gcpBillingAccountService.create(billingAccount));
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
    gcpBillingAccountService.delete(billingAccountId);
    return new RestResponse();
  }
}

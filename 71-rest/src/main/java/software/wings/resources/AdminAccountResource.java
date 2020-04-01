package software.wings.resources;

import com.google.inject.Inject;

import io.harness.datahandler.models.AccountSummary;
import io.harness.datahandler.services.AdminAccountService;
import io.harness.limits.ActionType;
import io.harness.limits.ConfiguredLimit;
import io.harness.limits.impl.model.RateLimit;
import io.harness.limits.impl.model.StaticLimit;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;
import software.wings.beans.Account;
import software.wings.beans.LicenseInfo;
import software.wings.beans.LicenseUpdateInfo;
import software.wings.security.annotations.AdminPortalAuth;
import software.wings.service.intfc.AccountService;

import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/admin/accounts")
@Slf4j
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@AdminPortalAuth
public class AdminAccountResource {
  private AdminAccountService adminAccountService;

  @Inject
  public AdminAccountResource(AdminAccountService adminAccountService, AccountService accountService) {
    this.adminAccountService = adminAccountService;
  }

  @GET
  @Path("summary")
  public RestResponse<List<AccountSummary>> getAccounts(
      @QueryParam("pageSize") Integer pageSize, @QueryParam("offset") String offset) {
    return new RestResponse<>(adminAccountService.getPaginatedAccountSummaries(offset, pageSize));
  }

  @GET
  @Path("summary/{accountId}")
  public RestResponse<AccountSummary> getAccountSummaryByAccountId(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(adminAccountService.getAccountSummaryByAccountId(accountId));
  }

  @GET
  @Path("{accountId}/license")
  public RestResponse<LicenseInfo> getLicenseInfoForAccount(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(adminAccountService.getLicense(accountId));
  }

  @PUT
  @Path("{accountId}/license")
  public RestResponse<LicenseInfo> updateAccountLicense(
      @PathParam("accountId") @NotEmpty String accountId, @NotNull LicenseUpdateInfo licenseUpdateInfo) {
    return new RestResponse<>(adminAccountService.updateLicense(accountId, licenseUpdateInfo));
  }

  @GET
  @Path("{accountId}/limits")
  public RestResponse<List<ConfiguredLimit>> getLimitsForAccount(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(adminAccountService.getLimitsConfiguredForAccount(accountId));
  }

  @PUT
  @Path("{accountId}/limits/static-limit")
  public RestResponse<ConfiguredLimit> configureStaticLimit(@PathParam("accountId") String accountId,
      @QueryParam("actionType") ActionType actionType, @Body StaticLimit limit) {
    return new RestResponse<>(adminAccountService.updateLimit(accountId, actionType, limit));
  }

  @PUT
  @Path("{accountId}/limits/rate-limit")
  public RestResponse<ConfiguredLimit> configureRateLimit(@PathParam("accountId") String accountId,
      @QueryParam("actionType") ActionType actionType, @Body RateLimit limit) {
    return new RestResponse<>(adminAccountService.updateLimit(accountId, actionType, limit));
  }

  @POST
  @Path("")
  public RestResponse<Account> createAccount(
      @Body Account account, @QueryParam("adminUserEmail") String adminUserEmail) {
    return new RestResponse<>(adminAccountService.createAccount(account, adminUserEmail));
  }

  @PUT
  @Path("/{accountId}/enable")
  public RestResponse<Boolean> enableAccount(@PathParam("accountId") String accountId) {
    return new RestResponse<>(adminAccountService.enableAccount(accountId));
  }

  @PUT
  @Path("/{accountId}/disable")
  public RestResponse<Boolean> disableAccount(
      @PathParam("accountId") String accountId, @QueryParam("newClusterUrl") String newClusterUrl) {
    return new RestResponse<>(adminAccountService.disableAccount(accountId, newClusterUrl));
  }

  @PUT
  @Path("/{accountId}/users/{userId}")
  public RestResponse<Boolean> enableOrDisableUser(@PathParam("accountId") String accountId,
      @PathParam("userId") String userId, @QueryParam("enable") boolean enabled) {
    return new RestResponse<>(adminAccountService.enableOrDisableUser(accountId, userId, enabled));
  }
}

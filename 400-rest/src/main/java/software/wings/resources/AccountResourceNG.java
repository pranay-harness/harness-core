package software.wings.resources;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.mappers.AccountMapper;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.Account;
import software.wings.helpers.ext.url.SubdomainUrlHelper;
import software.wings.security.authentication.TwoFactorAuthenticationManager;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.Size;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import retrofit2.http.Body;

@Api(value = "/ng/accounts", hidden = true)
@Path("/ng/accounts")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class AccountResourceNG {
  private final AccountService accountService;
  private SubdomainUrlHelper subdomainUrlHelper;
  private TwoFactorAuthenticationManager twoFactorAuthenticationManager;

  @GET
  @Path("{accountId}")
  public RestResponse<Account> get(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.get(accountId));
  }

  @GET
  @Path("/dto/{accountId}")
  public RestResponse<AccountDTO> getDTO(@PathParam("accountId") String accountId) {
    Account account = accountService.get(accountId);
    return new RestResponse<>(AccountMapper.toAccountDTO(account));
  }

  @GET
  @Path("/dto")
  public RestResponse<List<AccountDTO>> getDTOs(@QueryParam("accountIds") @Size(max = 100) List<String> accountIds) {
    List<Account> accounts = accountService.getAccounts(accountIds);
    return new RestResponse<>(accounts.stream().map(AccountMapper::toAccountDTO).collect(Collectors.toList()));
  }

  @GET
  @Path("/feature-flag-enabled")
  public RestResponse<Boolean> isFeatureFlagEnabled(
      @QueryParam("featureName") String featureName, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(accountService.isFeatureFlagEnabled(featureName, accountId));
  }

  @GET
  @Path("/baseUrl")
  public RestResponse<String> getBaseUrl(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(subdomainUrlHelper.getPortalBaseUrl(accountId));
  }

  @GET
  @Path("/get-whitelisted-domains")
  public RestResponse<Set<String>> getWhitelistedDomains(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.getWhitelistedDomains(accountId));
  }

  @PUT
  @Path("/whitelisted-domains")
  public RestResponse<Account> updateWhitelistedDomains(
      @QueryParam("accountId") @NotEmpty String accountId, @Body Set<String> whitelistedDomains) {
    return new RestResponse<>(accountService.updateWhitelistedDomains(accountId, whitelistedDomains));
  }

  @GET
  @Path("two-factor-enabled")
  public RestResponse<Boolean> getTwoFactorAuthAdminEnforceInfo(@QueryParam("accountId") @NotEmpty String accountId) {
    return new RestResponse(twoFactorAuthenticationManager.getTwoFactorAuthAdminEnforceInfo(accountId));
  }
}

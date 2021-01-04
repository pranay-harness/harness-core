package software.wings.resources;

import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import software.wings.beans.Account;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("/ng/accounts")
@Path("/ng/accounts")
@Produces("application/json")
@Consumes("application/json")
@NextGenManagerAuth
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class AccountResourceNG {
  private final AccountService accountService;

  @GET
  @Path("{accountId}")
  public RestResponse<Account> list(@PathParam("accountId") @NotEmpty String accountId) {
    return new RestResponse<>(accountService.get(accountId));
  }
}

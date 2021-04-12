package io.harness.account.resource;

import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.remote.client.RestClientUtils;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@OwnedBy(HarnessTeam.GTM)
@Api("/ng/account")
@Path("/ng/account")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
public class AccountResource {
  private final AccountClient accountClient;

  @Inject
  public AccountResource(AccountClient accountClient) {
    this.accountClient = accountClient;
  }

  @GET
  @Path("{accountIdentifier}")
  @ApiOperation(value = "Get Account", nickname = "getAccount")
  public ResponseDTO<AccountDTO> get(@PathParam("accountIdentifier") String accountIdentifier) {
    AccountDTO accountDTO = RestClientUtils.getResponse(accountClient.getAccountDTO(accountIdentifier));
    return ResponseDTO.newResponse(accountDTO);
  }
}
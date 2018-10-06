package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.security.annotations.PublicApi;
import software.wings.service.intfc.AccountService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api("account")
@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource {
  @Inject private AccountService accountService;

  @GET
  @Path("{accountId}/status")
  @Timed
  @ExceptionMetered
  @PublicApi
  public RestResponse<String> getStatus(@PathParam("accountId") String accountId) {
    return new RestResponse<>(accountService.getAccountStatus(accountId));
  }
}

package io.harness.ng.authenticationsettings.resources;

import io.harness.ng.authenticationsettings.dtos.AuthenticationSettingsResponse;
import io.harness.ng.authenticationsettings.impl.AuthenticationSettingsService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("authentication-settings")
@Path("/authentication-settings")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class AuthenticationSettingsResource {
  AuthenticationSettingsService authenticationSettingsService;

  @GET
  @Path("/")
  @Timed
  @ExceptionMetered
  public RestResponse<AuthenticationSettingsResponse> getAuthenticationSettings(
      @QueryParam("accountId") @NotEmpty String accountId) {
    AuthenticationSettingsResponse response = authenticationSettingsService.getAuthenticationSettings(accountId);
    return new RestResponse<>(response);
  }
}

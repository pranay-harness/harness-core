package io.harness.signup.resources;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.SignupService;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("/signup")
@Path("/signup")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@OwnedBy(GTM)
public class SignupResource {
  private SignupService signupService;

  /**
   * Follows the "free trial sign up" path
   * Module type can be optional but by default we will always redirect to NG
   * @param dto
   * @return
   */
  @PublicApi
  @POST
  public RestResponse<UserInfo> signup(SignupDTO dto) {
    return new RestResponse<>(signupService.signup(dto));
  }

  /**
   * Follows the "oauth" path
   * @param dto
   * @return
   */
  @POST
  @Path("/oauth")
  public RestResponse<UserInfo> signupOAuth(OAuthSignupDTO dto) {
    return new RestResponse<>(UserInfo.builder().build());
  }
}
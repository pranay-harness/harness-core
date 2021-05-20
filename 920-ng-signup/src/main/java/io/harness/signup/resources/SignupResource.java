package io.harness.signup.resources;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import static java.lang.Boolean.TRUE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.services.SignupService;

import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Api("signup")
@Path("signup")
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
  @POST
  @PublicApi
  public RestResponse<UserInfo> signup(SignupDTO dto, @QueryParam("captchaToken") @Nullable String captchaToken) {
    return new RestResponse<>(signupService.signup(dto, captchaToken));
  }

  /**
   * Follows the "oauth" path
   * @param dto
   * @return
   */
  @POST
  @Path("/oauth")
  @PublicApi
  public RestResponse<UserInfo> signupOAuth(OAuthSignupDTO dto) {
    return new RestResponse<>(signupService.oAuthSignup(dto));
  }

  @POST
  @Path("{userId}/verify-notification")
  @Produces("application/json")
  @Consumes("application/json")
  @ApiOperation(value = "Resend user verification email", nickname = "resendVerifyEmail")
  @AuthRule(permissionType = LOGGED_IN)
  public ResponseDTO<Boolean> resendVerifyEmail(@NotNull @PathParam("userId") String userId) {
    signupService.resendVerificationEmail(userId);
    return ResponseDTO.newResponse(TRUE);
  }
}

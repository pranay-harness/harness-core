package io.harness.signup.resources;

import com.google.inject.Inject;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.PublicApi;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.dto.VerifyTokenResponseDTO;
import io.harness.signup.services.SignupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static java.lang.Boolean.TRUE;

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
  private static final String X_FORWARDED_FOR = "X-Forwarded-For";

  private String acquireIpAddress(HttpServletRequest request) {
    String forwardedFor = request.getHeader(X_FORWARDED_FOR);
    return StringUtils.isNotBlank(forwardedFor) ? forwardedFor : request.getRemoteHost();
  }

  /**
   * Follows the "free trial sign up" path
   * Module type can be optional but by default we will always redirect to NG
   * @param dto
   * @return
   */
  @POST
  @PublicApi
  public RestResponse<Void> signup(SignupDTO dto, @QueryParam("captchaToken") @Nullable String captchaToken, @Context HttpServletRequest request) {
    String ipAddress = acquireIpAddress(request);
    signupService.createSignupInvite(dto, captchaToken, ipAddress);
    return new RestResponse<>();
  }

  /**
   * Follows the "free trial sign up" path
   * Module type can be optional but by default we will always redirect to NG
   * @param dto
   * @return
   */
  @POST
  @Path("/community")
  @PublicApi
  public RestResponse<UserInfo> communitySignup(SignupDTO dto) {
    return new RestResponse<>(signupService.communitySignup(dto));
  }

  @PUT
  @Path("/complete/{token}")
  @PublicApi
  public RestResponse<UserInfo> completeSignupInvite(@PathParam("token") String token, @Context HttpServletRequest request) {
    String ipAddress = acquireIpAddress(request);
    return new RestResponse<>(signupService.completeSignupInvite(token, ipAddress));
  }

  /**
   * Follows the "oauth" path
   *
   * @param dto
   * @return
   */
  @POST
  @Path("/oauth")
  @PublicApi
  public RestResponse<UserInfo> signupOAuth(OAuthSignupDTO dto, @Context HttpServletRequest request) {
    String ipAddress = acquireIpAddress(request);
    return new RestResponse<>(signupService.oAuthSignup(dto,ipAddress));
  }

  @POST
  @Path("/verify/{token}")
  @PublicApi
  public RestResponse<VerifyTokenResponseDTO> verifyToken(@PathParam("token") String token, @Context HttpServletRequest request) {
    String ipAddress = acquireIpAddress(request);
    return new RestResponse<>(signupService.verifyToken(token, ipAddress));
  }

  @POST
  @Path("verify-notification")
  @Produces("application/json")
  @Consumes("application/json")
  @ApiOperation(value = "Resend user verification email", nickname = "resendVerifyEmail")
  @PublicApi
  public ResponseDTO<Boolean> resendVerifyEmail(@NotNull @QueryParam("email") String email) {
    signupService.resendVerificationEmail(email);
    return ResponseDTO.newResponse(TRUE);
  }
}

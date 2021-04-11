
package io.harness.ng.userprofile.resource;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.user.TwoFactorAuthMechanismInfo;
import io.harness.ng.core.user.TwoFactorAuthSettingsInfo;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.userprofile.services.api.UserInfoService;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import retrofit2.http.Body;

@OwnedBy(HarnessTeam.PL)
@Api("user-info")
@Path("/user-info")
@Produces({"application/json"})
@Consumes({"application/json"})
@NextGenManagerAuth
public class UserInfoResource {
  @Inject UserInfoService userInfoService;

  @GET
  @ApiOperation(value = "get user information", nickname = "getUserInfo")
  public ResponseDTO<UserInfo> getUserInfo() throws IOException {
    return ResponseDTO.newResponse(userInfoService.get());
  }

  @PUT
  @ApiOperation(value = "update user information", nickname = "updateUserInfo")
  public ResponseDTO<UserInfo> updateUserInfo(@Body UserInfo userInfo) throws IOException {
    return ResponseDTO.newResponse(userInfoService.update(userInfo));
  }

  @GET
  @Path("two-factor-auth/{auth-mechanism}")
  @ApiOperation(value = "get two factor auth settings", nickname = "getTwoFactorAuthSettings")
  public ResponseDTO<TwoFactorAuthSettingsInfo> getTwoFactorAuthSettingsInfo(
      @PathParam("auth-mechanism") TwoFactorAuthMechanismInfo authMechanism) {
    return ResponseDTO.newResponse(userInfoService.getTwoFactorAuthSettingsInfo(authMechanism));
  }

  @PUT
  @Path("enable-two-factor-auth")
  @ApiOperation(value = "enable two factor auth settings", nickname = "enableTwoFactorAuth")
  public ResponseDTO<UserInfo> updateTwoFactorAuthInfo(@Body TwoFactorAuthSettingsInfo authSettingsInfo) {
    return ResponseDTO.newResponse(userInfoService.updateTwoFactorAuthInfo(authSettingsInfo));
  }

  @PUT
  @Path("disable-two-factor-auth")
  @ApiOperation(value = "disable two factor auth settings", nickname = "disableTwoFactorAuth")
  public ResponseDTO<UserInfo> disableTFA() {
    return ResponseDTO.newResponse(userInfoService.disableTFA());
  }
}

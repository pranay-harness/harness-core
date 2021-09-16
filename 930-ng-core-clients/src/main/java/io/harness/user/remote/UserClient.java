package io.harness.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.dto.UserInviteDTO;
import io.harness.ng.core.user.PasswordChangeDTO;
import io.harness.ng.core.user.PasswordChangeResponse;
import io.harness.ng.core.user.TwoFactorAuthMechanismInfo;
import io.harness.ng.core.user.TwoFactorAuthSettingsInfo;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.rest.RestResponse;
import io.harness.signup.dto.SignupInviteDTO;

import java.util.List;
import java.util.Optional;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface UserClient {
  String SEARCH_TERM_KEY = "searchTerm";
  String USERS_SEARCH_API = "ng/user/search";
  String USERS_API = "ng/user";
  String USERS_API_OAUTH = "ng/user/oauth";
  String USERS_SIGNUP_INVITE_API = "ng/user/signup-invite";
  String USER_BATCH_LIST_API = "ng/user/batch";
  String USER_IN_ACCOUNT_VERIFICATION = "ng/user/user-account";
  String USER_SAFE_DELETE = "ng/user/safeDelete/{userId}";
  String UPDATE_USER_API = "ng/user/user";
  String CREATE_USER_VIA_INVITE = "ng/user/invites/create-user";
  String USER_TWO_FACTOR_AUTH_SETTINGS = "ng/user/two-factor-auth/{auth-mechanism}";
  String USER_ENABLE_TWO_FACTOR_AUTH = "ng/user/enable-two-factor-auth";
  String USER_DISABLE_TWO_FACTOR_AUTH = "ng/user/disable-two-factor-auth";
  String USER_UNLOCK = "ng/user/unlock-user";
  String ALL_PROJECTS_ACCESSIBLE_TO_USER_API = "ng/user/all-projects";

  @POST(USERS_API) Call<RestResponse<UserInfo>> createNewUser(@Body UserRequestDTO userRequest);

  @POST(USERS_API_OAUTH) Call<RestResponse<UserInfo>> createNewOAuthUser(@Body UserRequestDTO userRequest);

  @POST(USERS_SIGNUP_INVITE_API)
  Call<RestResponse<SignupInviteDTO>> createNewSignupInvite(@Body SignupInviteDTO userRequest);

  @GET(USERS_SIGNUP_INVITE_API) Call<RestResponse<SignupInviteDTO>> getSignupInvite(@Query("email") String email);

  @PUT(USERS_SIGNUP_INVITE_API) Call<RestResponse<UserInfo>> completeSignupInvite(@Query("email") String email);

  @GET(USERS_SEARCH_API)
  Call<RestResponse<PageResponse<UserInfo>>> list(@Query(value = "accountId") String accountId,
      @Query("offset") String offset, @Query("limit") String limit, @Query("searchTerm") String searchTerm,
      @Query("requireAdminStatus") boolean requireAdminStatus);

  @GET(USERS_API + "/{userId}") Call<RestResponse<Optional<UserInfo>>> getUserById(@Path("userId") String userId);

  @GET(USERS_API + "/email/{emailId}")
  Call<RestResponse<Optional<UserInfo>>> getUserByEmailId(@Path("emailId") String emailId);

  @POST(USER_BATCH_LIST_API)
  Call<RestResponse<List<UserInfo>>> listUsers(@Query("accountId") String accountId, @Body UserFilterNG userFilterNG);

  @PUT(UPDATE_USER_API) Call<RestResponse<Optional<UserInfo>>> updateUser(@Body UserInfo userInfo);

  @PUT(CREATE_USER_VIA_INVITE)
  Call<RestResponse<Boolean>> createUserAndCompleteNGInvite(@Body UserInviteDTO userInviteDTO);

  @GET(USER_IN_ACCOUNT_VERIFICATION)
  Call<RestResponse<Boolean>> isUserInAccount(
      @Query(value = "accountId") String accountId, @Query(value = "userId") String userId);

  @POST(USER_IN_ACCOUNT_VERIFICATION)
  Call<RestResponse<Boolean>> addUserToAccount(
      @Query(value = "userId") String userId, @Query(value = "accountId") String accountId);

  @DELETE(USER_SAFE_DELETE)
  Call<RestResponse<Boolean>> safeDeleteUser(
      @Path(value = "userId") String userId, @Query(value = "accountId") String accountId);

  @GET(USER_TWO_FACTOR_AUTH_SETTINGS)
  Call<RestResponse<Optional<TwoFactorAuthSettingsInfo>>> getUserTwoFactorAuthSettings(
      @Path(value = "auth-mechanism") TwoFactorAuthMechanismInfo authMechanism,
      @Query(value = "emailId") String emailId);

  @PUT(USER_ENABLE_TWO_FACTOR_AUTH)
  Call<RestResponse<Optional<UserInfo>>> updateUserTwoFactorAuthInfo(
      @Query(value = "emailId") String emailId, @Body TwoFactorAuthSettingsInfo settings);

  @PUT(USER_DISABLE_TWO_FACTOR_AUTH)
  Call<RestResponse<Optional<UserInfo>>> disableUserTwoFactorAuth(@Query(value = "emailId") String emailId);

  @GET(USERS_API + "/user-password-present")
  Call<RestResponse<Boolean>> isUserPasswordSet(@Query("accountId") String accountId, @Query("emailId") String emailId);

  @PUT(USERS_API + "/password")
  Call<RestResponse<PasswordChangeResponse>> changeUserPassword(
      @Query(value = "userId") String userId, @Body PasswordChangeDTO password);

  @PUT(USERS_API + "/{userId}/verified")
  Call<RestResponse<Boolean>> changeUserEmailVerified(@Path(value = "userId") String userId);

  @PUT(USER_UNLOCK)
  Call<RestResponse<Optional<UserInfo>>> unlockUser(
      @Query(value = "email") String email, @Query("accountId") String accountId);

  @GET(ALL_PROJECTS_ACCESSIBLE_TO_USER_API)
  Call<RestResponse<List<ProjectDTO>>> getUserAllProjectsInfo(
      @Query(value = "accountId") String accountId, @Query(value = "userId") String userId);
}

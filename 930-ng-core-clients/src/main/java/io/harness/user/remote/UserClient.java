package io.harness.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.core.user.TwoFactorAuthMechanismInfo;
import io.harness.ng.core.user.TwoFactorAuthSettingsInfo;
import io.harness.ng.core.user.UserInfo;
import io.harness.rest.RestResponse;

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
  String USER_BATCH_LIST_API = "ng/user/batch";
  String USER_IN_ACCOUNT_VERIFICATION = "ng/user/user-account";
  String USER_SAFE_DELETE = "ng/user/safeDelete/{userId}";
  String UPDATE_USER_API = "ng/user/user";
  String USER_TWO_FACTOR_AUTH_SETTINGS = "ng/user/two-factor-auth/{auth-mechanism}";
  String USER_ENABLE_TWO_FACTOR_AUTH = "ng/user/enable-two-factor-auth";
  String USER_DISABLE_TWO_FACTOR_AUTH = "ng/user/disable-two-factor-auth";

  @GET(USERS_SEARCH_API)
  Call<RestResponse<PageResponse<UserInfo>>> list(@Query(value = "accountId") String accountId,
      @Query("offset") String offset, @Query("limit") String limit, @Query("searchTerm") String searchTerm);

  @GET(USERS_API + "/{userId}") Call<RestResponse<Optional<UserInfo>>> getUserById(@Path("userId") String userId);

  @GET(USERS_API + "/email/{emailId}")
  Call<RestResponse<Optional<UserInfo>>> getUserByEmailId(@Path("emailId") String emailId);

  @POST(USER_BATCH_LIST_API)
  Call<RestResponse<List<UserInfo>>> listUsers(
      @Body UserSearchFilter userSearchFilter, @Query("accountId") String accountId);

  @PUT(UPDATE_USER_API) Call<RestResponse<Optional<UserInfo>>> updateUser(@Body UserInfo userInfo);

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
}

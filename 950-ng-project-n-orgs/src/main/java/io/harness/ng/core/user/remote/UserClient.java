package io.harness.ng.core.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.core.user.User;
import io.harness.rest.RestResponse;

import java.util.List;
import java.util.Optional;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface UserClient {
  String OFFSET_KEY = "offset";
  String LIMIT_KEY = "limit";
  String SEARCH_TERM_KEY = "searchTerm";
  String USERS_SEARCH_API = "ng/users/search";
  String USERS_API = "ng/users";
  String USERNAME_API = "ng/users/usernames";
  String USER_BATCH_LIST_API = "ng/users/batch";
  String USER_IN_ACCOUNT_VERIFICATION = "ng/users/user-account";

  @GET(USERS_SEARCH_API)
  Call<RestResponse<PageResponse<User>>> list(@Query(value = "accountId") String accountId,
      @Query("offset") String offset, @Query("limit") String limit, @Query("searchTerm") String searchTerm);

  @GET(USERNAME_API)
  Call<RestResponse<List<String>>> getUsernameFromEmail(
      @Query(value = "accountId") String accountId, @Query(value = "emailList") List<String> emailList);

  @GET(USERS_API)
  Call<RestResponse<Optional<User>>> getUserFromEmail(
      @Query(value = "accountId") String accountId, @Query(value = "emailId") String email);

  @POST(USER_BATCH_LIST_API) Call<RestResponse<List<User>>> getUsersByIds(@Body List<String> userIds);

  @GET(USER_IN_ACCOUNT_VERIFICATION)
  Call<RestResponse<Boolean>> isUserInAccount(
      @Query(value = "accountId") String accountId, @Query(value = "userId") String userId);
}

package io.harness.ng.core.user.remote;

import io.harness.beans.PageResponse;
import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import software.wings.beans.User;

import java.util.List;

public interface UserClient {
  String ACCOUNT_ID_KEY = "accountId";
  String OFFSET_KEY = "offset";
  String LIMIT_KEY = "limit";
  String SEARCH_TERM_KEY = "searchTerm";
  String EMAIL_LIST = "emailList";
  String USERS_API = "/api/ng/users";
  String USERNAME_API = "/api/ng/users/usernames";

  @GET(USERS_API)
  Call<RestResponse<PageResponse<User>>> list(@Query(value = ACCOUNT_ID_KEY) String accountId,
      @Query(OFFSET_KEY) String offset, @Query(LIMIT_KEY) String limit, @Query(SEARCH_TERM_KEY) String searchTerm);

  @GET(USERNAME_API)
  Call<RestResponse<List<String>>> getUsernameFromEmail(
      @Query(value = ACCOUNT_ID_KEY) String accountId, @Query(value = EMAIL_LIST) List<String> emailList);
}

package io.harness.ng.core.user.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.ng.core.user.User;
import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import java.util.List;
import java.util.Optional;

@OwnedBy(PL)
public interface UserClient {
  String ACCOUNT_ID_KEY = "accountId";
  String OFFSET_KEY = "offset";
  String LIMIT_KEY = "limit";
  String SEARCH_TERM_KEY = "searchTerm";
  String EMAIL_LIST = "emailList";
  String EMAIL_ID = "emailId";
  String USERS_SEARCH_API = "ng/users/search";
  String USERS_API = "ng/users";
  String USERNAME_API = "ng/users/usernames";

  @GET(USERS_SEARCH_API)
  Call<RestResponse<PageResponse<User>>> list(@Query(value = ACCOUNT_ID_KEY) String accountId,
      @Query(OFFSET_KEY) String offset, @Query(LIMIT_KEY) String limit, @Query(SEARCH_TERM_KEY) String searchTerm);

  @GET(USERNAME_API)
  Call<RestResponse<List<String>>> getUsernameFromEmail(
      @Query(value = ACCOUNT_ID_KEY) String accountId, @Query(value = EMAIL_LIST) List<String> emailList);

  @GET(USERS_API)
  Call<RestResponse<Optional<User>>> getUserFromEmail(
      @Query(value = ACCOUNT_ID_KEY) String accountId, @Query(value = EMAIL_ID) String email);
}

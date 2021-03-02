package io.harness.ng.core.account.remote;

import io.harness.ng.core.dto.AccountDTO;
import io.harness.rest.RestResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AccountClient {
  String ACCOUNT_DTO_API = "ng/accounts/dto";

  @GET(ACCOUNT_DTO_API + "/{accountId}")
  Call<RestResponse<AccountDTO>> getAccountDTO(@Path("accountId") String accountId);

  @GET(ACCOUNT_DTO_API)
  Call<RestResponse<List<AccountDTO>>> getAccountDTOs(@Query("accountIds") List<String> accountIds);
}

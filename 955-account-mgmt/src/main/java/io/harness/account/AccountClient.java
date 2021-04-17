package io.harness.account;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.rest.RestResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface AccountClient {
  String ACCOUNT_DTO_API = "ng/accounts/dto";
  String FEATURE_FLAG_CHECK_API = "ng/accounts/feature-flag-enabled";
  String ACCOUNT_BASEURL = "ng/accounts/baseUrl";

  @GET(ACCOUNT_DTO_API + "/{accountId}")
  Call<RestResponse<AccountDTO>> getAccountDTO(@Path("accountId") String accountId);

  @GET(ACCOUNT_DTO_API)
  Call<RestResponse<List<AccountDTO>>> getAccountDTOs(@Query("accountIds") List<String> accountIds);

  @GET(FEATURE_FLAG_CHECK_API)
  Call<RestResponse<Boolean>> isFeatureFlagEnabled(
      @Query("featureName") String featureName, @Query("accountId") String accountId);

  @GET(ACCOUNT_BASEURL) Call<RestResponse<String>> getBaseUrl(@Query("accountId") String accountId);
}

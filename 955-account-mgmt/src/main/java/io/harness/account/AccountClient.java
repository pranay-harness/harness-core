package io.harness.account;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.rest.RestResponse;

import java.util.Collection;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface AccountClient {
  String ACCOUNT_API = "ng/accounts";
  String FEATURE_FLAG_CHECK_API = "ng/accounts/feature-flag-enabled";
  String ACCOUNT_BASEURL_API = "ng/accounts/baseUrl";
  String ACCOUNT_EXISTS = "ng/accounts/exists";
  String ACCOUNT_ADMIN_API = ACCOUNT_API + "/account-admins";
  String FEATURE_FLAG_LIST_API = "ng/user/feature-flags/{accountId}";

  @POST(ACCOUNT_API) Call<RestResponse<AccountDTO>> create(@Body AccountDTO dto);

  @GET(ACCOUNT_API + "/{accountId}") Call<RestResponse<AccountDTO>> getAccountDTO(@Path("accountId") String accountId);

  @GET(ACCOUNT_API) Call<RestResponse<List<AccountDTO>>> getAccountDTOs(@Query("accountIds") List<String> accountIds);

  @GET(FEATURE_FLAG_CHECK_API)
  Call<RestResponse<Boolean>> isFeatureFlagEnabled(
      @Query("featureName") String featureName, @Query("accountId") String accountId);

  @GET(ACCOUNT_BASEURL_API) Call<RestResponse<String>> getBaseUrl(@Query("accountId") String accountId);

  @GET(ACCOUNT_ADMIN_API) Call<RestResponse<List<String>>> getAccountAdmins(@Query("accountId") String accountId);

  @GET(ACCOUNT_EXISTS + "/{accountName}")
  Call<RestResponse<Boolean>> doesAccountExist(@Path("accountName") String accountName);

  @PUT(ACCOUNT_API + "/{accountId}/default-experience")
  Call<RestResponse<Boolean>> updateDefaultExperienceIfApplicable(
      @Path("accountId") String accountId, @Query("defaultExperience") DefaultExperience defaultExperience);

  @GET(FEATURE_FLAG_LIST_API)
  Call<RestResponse<Collection<FeatureFlag>>> listAllFeatureFlagsForAccount(@Path("accountId") String accountId);
}

/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.account;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.ng.core.account.DefaultExperience;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.user.UserMetadata;
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
  String ACCOUNT_GATEWAYBASEURL_API = "ng/accounts/gatewayBaseUrl";
  String ACCOUNT_EXISTS = "ng/accounts/exists";
  String ACCOUNT_ADMIN_API = ACCOUNT_API + "/account-admins";
  String FEATURE_FLAG_LIST_API = "ng/user/feature-flags/{accountId}";
  String HARNESS_USER_GROUP_API = "harnessUserGroup";

  @POST(ACCOUNT_API) Call<RestResponse<AccountDTO>> create(@Body AccountDTO dto);

  @GET(ACCOUNT_API + "/{accountId}") Call<RestResponse<AccountDTO>> getAccountDTO(@Path("accountId") String accountId);

  @GET(ACCOUNT_API) Call<RestResponse<List<AccountDTO>>> getAccountDTOs(@Query("accountIds") List<String> accountIds);

  @PUT(ACCOUNT_API + "/{accountId}/name")
  Call<RestResponse<AccountDTO>> updateAccountName(@Path("accountId") String accountId, @Body AccountDTO dto);

  @GET(FEATURE_FLAG_CHECK_API)
  Call<RestResponse<Boolean>> isFeatureFlagEnabled(
      @Query("featureName") String featureName, @Query("accountId") String accountId);

  @GET(ACCOUNT_API + "/{accountId}/nextgen-enabled")
  Call<RestResponse<Boolean>> isNextGenEnabled(@Path("accountId") String accountId);

  @GET(ACCOUNT_BASEURL_API) Call<RestResponse<String>> getBaseUrl(@Query("accountId") String accountId);

  @GET(ACCOUNT_GATEWAYBASEURL_API) Call<RestResponse<String>> getGatewayBaseUrl(@Query("accountId") String accountId);

  @GET(ACCOUNT_ADMIN_API) Call<RestResponse<List<String>>> getAccountAdmins(@Query("accountId") String accountId);

  @GET(ACCOUNT_EXISTS + "/{accountName}")
  Call<RestResponse<Boolean>> doesAccountExist(@Path("accountName") String accountName);

  @PUT(ACCOUNT_API + "/{accountId}/default-experience-if-applicable")
  Call<RestResponse<Boolean>> updateDefaultExperienceIfApplicable(
      @Path("accountId") String accountId, @Query("defaultExperience") DefaultExperience defaultExperience);

  @PUT(ACCOUNT_API + "/{accountId}/default-experience")
  Call<RestResponse<AccountDTO>> updateDefaultExperience(@Path("accountId") String accountId, @Body AccountDTO dto);

  @GET(FEATURE_FLAG_LIST_API)
  Call<RestResponse<Collection<FeatureFlag>>> listAllFeatureFlagsForAccount(@Path("accountId") String accountId);

  @GET(HARNESS_USER_GROUP_API + "/supportUsers") Call<RestResponse<List<UserMetadata>>> listAllHarnessSupportUsers();

  @GET(HARNESS_USER_GROUP_API + "/supportEnabledStatus")
  Call<RestResponse<Boolean>> checkIfHarnessSupportEnabledForAccount(@Query("accountId") String accountId);

  @GET(ACCOUNT_API + "/isAutoInviteAcceptanceEnabled")
  Call<RestResponse<Boolean>> checkAutoInviteAcceptanceEnabledForAccount(@Query("accountId") String accountId);
}

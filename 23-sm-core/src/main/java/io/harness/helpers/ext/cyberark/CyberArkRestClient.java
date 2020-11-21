package io.harness.helpers.ext.cyberark;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@OwnedBy(PL)
public interface CyberArkRestClient {
  String BASE_CYBERARK_URL = "AIMWebService/api/Accounts";

  @GET(BASE_CYBERARK_URL)
  Call<CyberArkReadResponse> readSecret(@Query("AppID") String appId, @Query("Query") String query);
}

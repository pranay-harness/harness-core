package software.wings.helpers.ext.cyberark;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import software.wings.service.impl.security.cyberark.CyberArkReadResponse;

public interface CyberArkRestClient {
  String BASE_CYBERARK_URL = "AIMWebService/api/Accounts";

  @GET(BASE_CYBERARK_URL)
  Call<CyberArkReadResponse> readSecret(@Query("AppID") String appId, @Query("Query") String query);
}

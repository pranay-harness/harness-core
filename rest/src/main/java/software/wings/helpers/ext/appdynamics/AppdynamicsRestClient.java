package software.wings.helpers.ext.appdynamics;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import software.wings.service.impl.appdynamics.AppdynamicsApplication;
import software.wings.service.impl.appdynamics.AppdynamicsBusinessTransaction;
import software.wings.service.impl.appdynamics.AppdynamicsTier;

import java.util.List;

/**
 * Created by rsingh on 4/17/17.
 */
public interface AppdynamicsRestClient {
  /**
   * List all the applications of appdynamics
   *
   * @param authorization the authorization
   * @return the call
   */
  @GET("rest/applications?output=json")
  Call<List<AppdynamicsApplication>> listAllApplications(@Header("Authorization") String authorization);

  @GET("rest/applications/{appdynamicsAppId}/tiers?output=json")
  Call<List<AppdynamicsTier>> listTiers(
      @Header("Authorization") String authorization, @Path("appdynamicsAppId") long appdynamicsAppId);

  @GET("rest/applications/{appdynamicsAppId}/business-transactions?output=json")
  Call<List<AppdynamicsBusinessTransaction>> listBusinessTransactions(
      @Header("Authorization") String authorization, @Path("appdynamicsAppId") long appdynamicsAppId);
}

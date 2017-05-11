package software.wings.helpers.ext.appdynamics;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import software.wings.service.impl.appdynamics.AppdynamicsApplicationResponse;

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
  Call<List<AppdynamicsApplicationResponse>> listAllApplications(@Header("Authorization") String authorization);
}

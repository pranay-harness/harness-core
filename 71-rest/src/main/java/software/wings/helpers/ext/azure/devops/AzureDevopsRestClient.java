package software.wings.helpers.ext.azure.devops;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

@OwnedBy(CDC)
public interface AzureDevopsRestClient {
  @GET("_apis/projects?api-version=5.1")
  Call<AzureDevopsProjects> listProjects(@Header("Authorization") String authHeader);
}

package software.wings.helpers.ext.azure.devops;

import static io.harness.annotations.dev.HarnessModule._960_API_SERVICES;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

@OwnedBy(CDC)
@TargetModule(_960_API_SERVICES)
public interface AzureDevopsRestClient {
  @GET("_apis/projects?api-version=5.1")
  Call<AzureDevopsProjects> listProjects(@Header("Authorization") String authHeader);
}

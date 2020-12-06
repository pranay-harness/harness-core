package software.wings.helpers.ext.azure;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface AzureManagementRestClient {
  @GET(
      "/subscriptions/{subscriptionId}/resourcegroups/{resourceGroup}/providers/Microsoft.ContainerService/managedClusters/{clusterName}/accessProfiles/clusterAdmin?api-version=2017-08-31")
  Call<AksGetCredentialsResponse>
  getAdminCredentials(@Header("Authorization") String bearerAuthHeader,
      @Path(value = "subscriptionId", encoded = true) String subscriptionId,
      @Path(value = "resourceGroup", encoded = true) String resourceGroup,
      @Path(value = "clusterName", encoded = true) String clusterName);

  @GET("subscriptions/{subscriptionId}/tagNames?api-version=2016-09-01")
  Call<AzureListTagsResponse> listTags(
      @Header("Authorization") String bearerAuthHeader, @Path("subscriptionId") String subscriptionId);
}

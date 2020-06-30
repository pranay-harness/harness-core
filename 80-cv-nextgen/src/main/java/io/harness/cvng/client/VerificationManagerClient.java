package io.harness.cvng.client;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_DATA_COLLECTION_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.CV_NEXTGEN_RESOURCE_PREFIX;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_HISTOGRAM_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_RESOURCE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_SAMPLE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_SAVED_SEARCH_PATH;

import io.harness.cvng.beans.CVHistogram;
import io.harness.cvng.beans.SplunkSampleResponse;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;
import javax.ws.rs.container.ContainerRequestContext;

public interface VerificationManagerClient {
  @GET("account/feature-flag-enabled")
  Call<RestResponse<Boolean>> isFeatureEnabled(
      @Query("featureName") String featureName, @Query("accountId") String accountId);

  @POST(CV_DATA_COLLECTION_PATH + "/create-task")

  Call<RestResponse<String>> createDataCollectionTask(@Query("accountId") String accountId,
      @Query("cvConfigId") String cvConfigId, @Query("connectorId") String connectorId);
  @GET(SPLUNK_RESOURCE_PATH + SPLUNK_SAVED_SEARCH_PATH)
  Call<RestResponse<List<SplunkSavedSearch>>> getSavedSearches(
      @Query("accountId") String accountId, @Query("connectorId") String connectorId);

  @GET(SPLUNK_RESOURCE_PATH + SPLUNK_HISTOGRAM_PATH)
  Call<RestResponse<CVHistogram>> getHistogram(
      @Query("accountId") String accountId, @Query("connectorId") String connectorId, @Query("query") String query);

  @GET(SPLUNK_RESOURCE_PATH + SPLUNK_SAMPLE_PATH)
  Call<RestResponse<SplunkSampleResponse>> getSamples(
      @Query("accountId") String accountId, @Query("connectorId") String connectorId, @Query("query") String query);

  @GET(CV_NEXTGEN_RESOURCE_PREFIX + "/auth/validate-token")
  Call<RestResponse<Boolean>> authenticateUser(
      @Query("containerRequestContext") ContainerRequestContext containerRequestContext);
}

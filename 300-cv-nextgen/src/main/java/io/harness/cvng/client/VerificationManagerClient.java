package io.harness.cvng.client;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_DATA_COLLECTION_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.KUBERNETES_RESOURCE;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_RESOURCE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_SAVED_SEARCH_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.SPLUNK_VALIDATION_RESPONSE_PATH;

import io.harness.cvng.beans.AppdynamicsValidationResponse;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.SplunkSavedSearch;
import io.harness.cvng.beans.SplunkValidationResponse;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.beans.appd.AppDynamicsTier;
import io.harness.cvng.beans.appd.AppdynamicsMetricPackDataValidationRequest;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.rest.RestResponse;

import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface VerificationManagerClient {
  @GET("account/feature-flag-enabled")
  Call<RestResponse<Boolean>> isFeatureEnabled(
      @Query("featureName") String featureName, @Query("accountId") String accountId);

  @POST(CV_DATA_COLLECTION_PATH + "/create-task")
  Call<RestResponse<String>> createDataCollectionPerpetualTask(@Query("accountId") String accountId,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Body DataCollectionConnectorBundle bundle);

  @DELETE(CV_DATA_COLLECTION_PATH + "/delete-task")
  Call<RestResponse<Void>> deleteDataCollectionPerpetualTask(
      @Query("accountId") String accountId, @Query("taskId") String taskId);

  @POST(CV_DATA_COLLECTION_PATH + "/get-data-collection-result")
  Call<RestResponse<String>> getDataCollectionResponse(@Query("accountId") String accountId,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Body DataCollectionRequest request);

  @POST(SPLUNK_RESOURCE_PATH + SPLUNK_SAVED_SEARCH_PATH)
  Call<RestResponse<List<SplunkSavedSearch>>> getSavedSearches(@Query("accountId") String accountId,
      @Query("connectorId") String connectorId, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier, @Query("requestGuid") String requestGuid,
      @Body SplunkConnectorDTO splunkConnectorDTO);

  @POST(SPLUNK_RESOURCE_PATH + SPLUNK_VALIDATION_RESPONSE_PATH)
  Call<RestResponse<SplunkValidationResponse>> getSamples(@Query("accountId") String accountId,
      @Query("connectorId") String connectorId, @Query("orgIdentifier") String orgIdentifier,
      @Query("projectIdentifier") String projectIdentifier, @Query("query") String query,
      @Query("requestGuid") String requestGuid, @Body SplunkConnectorDTO splunkConnectorDTO);

  @POST("appdynamics/metric-data")
  Call<RestResponse<Set<AppdynamicsValidationResponse>>> getAppDynamicsMetricData(@Query("accountId") String accountId,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Query("appName") String appName, @Query("tierName") String tierName, @Query("requestGuid") String requestGuid,
      @Body AppdynamicsMetricPackDataValidationRequest validationRequest);

  @POST("appdynamics/applications-ng")
  Call<RestResponse<List<AppDynamicsApplication>>> getAppDynamicsApplications(@Query("accountId") String accountId,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Body AppDynamicsConnectorDTO appDynamicsConnectorDTO);

  @POST("appdynamics/tiers-ng")
  Call<RestResponse<Set<AppDynamicsTier>>> getTiers(@Query("accountId") String accountId,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Query("appDynamicsAppId") long appDynamicsAppId, @Body AppDynamicsConnectorDTO appDynamicsConnectorDTO);

  @POST("account/validate-delegate-token")
  Call<RestResponse<Boolean>> authenticateDelegateRequest(
      @Query("accountId") String accountId, @Query("delegateToken") String delegateToken);

  @POST(KUBERNETES_RESOURCE + "/namespaces")
  Call<RestResponse<List<String>>> getKubernetesNamespaces(@Query("accountId") String accountId,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Query("filter") String filter, @Body DataCollectionConnectorBundle bundle);

  @POST(KUBERNETES_RESOURCE + "/workloads")
  Call<RestResponse<List<String>>> getKubernetesWorkloads(@Query("accountId") String accountId,
      @Query("orgIdentifier") String orgIdentifier, @Query("projectIdentifier") String projectIdentifier,
      @Query("namespace") String namespace, @Query("filter") String filter, @Body DataCollectionConnectorBundle bundle);
}

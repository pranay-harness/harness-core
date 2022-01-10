/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.helpers.ext.newrelic;

import software.wings.beans.NewRelicDeploymentMarkerPayload;
import software.wings.service.impl.newrelic.NewRelicApplicationInstancesResponse;
import software.wings.service.impl.newrelic.NewRelicApplicationsResponse;
import software.wings.service.impl.newrelic.NewRelicMetricDataResponse;
import software.wings.service.impl.newrelic.NewRelicMetricResponse;

import java.util.Collection;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Created by rsingh on 8/28/17.
 */
public interface NewRelicRestClient {
  String NAMES_PARAM = "names[]";
  String FILTER_NAME_PARAM = "filter[name]";
  String FILTER_IDS_PARAM = "filter[ids]";
  /**
   * Lists all the applications of new relic
   *
   * @return the call
   */
  @GET("v2/applications.json")
  Call<NewRelicApplicationsResponse> listAllApplications(
      @Header("X-Api-Key") String apiKey, @Query("page") int pageCount);

  @GET("v2/applications.json")
  Call<NewRelicApplicationsResponse> listAllApplicationsByNameFilter(
      @Header("X-Api-Key") String apiKey, @Query(FILTER_NAME_PARAM) String newRelicApplicationName);

  @GET("v2/applications.json")
  Call<NewRelicApplicationsResponse> listAllApplicationsByIdFilter(
      @Header("X-Api-Key") String apiKey, @Query(FILTER_IDS_PARAM) String newRelicApplicationId);

  @GET("v2/applications/{applicationId}/instances.json")
  Call<NewRelicApplicationInstancesResponse> listAppInstances(
      @Header("X-Api-Key") String apiKey, @Path("applicationId") long newRelicAppId, @Query("page") int pageCount);

  @GET("v2/applications/{applicationId}/metrics/data.json")
  Call<NewRelicMetricDataResponse> getApplicationMetricData(@Header("X-Api-Key") String apiKey,
      @Path("applicationId") long applicationId, @Query("summarize") boolean summarize, @Query("from") String fromTime,
      @Query("to") String toTime, @Query(NAMES_PARAM) Collection<String> metricNames);

  @GET("v2/applications/{applicationId}/instances/{instanceId}/metrics/data.json")
  Call<NewRelicMetricDataResponse> getInstanceMetricData(@Header("X-Api-Key") String apiKey,
      @Path("applicationId") long applicationId, @Path("instanceId") long instanceId, @Query("from") String fromTime,
      @Query("to") String toTime, @Query(NAMES_PARAM) Collection<String> metricNames);

  @GET("v2/applications/{applicationId}/metrics.json")
  Call<NewRelicMetricResponse> listMetricNames(
      @Header("X-Api-Key") String apiKey, @Path("applicationId") long newRelicAppId, @Query("name") String txnName);

  @POST()
  Call<Object> postDeploymentMarker(
      @Header("X-Api-Key") String apiKey, @Url String url, @Body NewRelicDeploymentMarkerPayload body);
}

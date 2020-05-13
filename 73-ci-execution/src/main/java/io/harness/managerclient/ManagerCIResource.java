package io.harness.managerclient;

import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.POST;
import retrofit2.http.Query;
import software.wings.beans.ci.K8ExecCommandParams;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.common.CICommonEndpointConstants;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

/**
 * Temporary Manager Client for helping CI to send delegate tasks from CI to CD
 */

public interface ManagerCIResource {
  @POST("ci" + CICommonEndpointConstants.CI_SETUP_ENDPOINT)
  @KryoRequest
  Call<RestResponse<K8sTaskExecutionResponse>> createK8PodTask(@Query("k8ConnectorName") String k8ConnectorName,
      @Query("gitConnectorName") String gitConnectorName, @Body CIK8PodParams<CIK8ContainerParams> podParams);

  @POST("ci" + CICommonEndpointConstants.CI_COMMAND_EXECUTION_ENDPOINT)
  @KryoRequest
  Call<RestResponse<K8sTaskExecutionResponse>> podCommandExecutionTask(
      @Query("k8ConnectorName") String k8ConnectorName, @Body K8ExecCommandParams params);

  @DELETE("ci" + CICommonEndpointConstants.CI_CLEANUP_ENDPOINT)
  @KryoRequest
  Call<RestResponse<K8sTaskExecutionResponse>> podCleanupTask(@Query("k8ConnectorName") String k8ConnectorName);
}

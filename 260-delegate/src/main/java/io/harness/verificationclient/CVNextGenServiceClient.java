package io.harness.verificationclient;

import static io.harness.cvng.core.services.CVNextGenConstants.CVNG_LOG_RESOURCE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.DELEGATE_DATA_COLLECTION;
import static io.harness.cvng.core.services.CVNextGenConstants.DELEGATE_DATA_COLLECTION_TASK;
import static io.harness.cvng.core.services.CVNextGenConstants.HOST_RECORD_RESOURCE_PATH;
import static io.harness.cvng.core.services.CVNextGenConstants.KUBERNETES_RESOURCE;
import static io.harness.cvng.core.services.CVNextGenConstants.LOG_RECORD_RESOURCE_PATH;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.beans.DataCollectionTaskDTO;
import io.harness.cvng.beans.DataCollectionTaskDTO.DataCollectionTaskResult;
import io.harness.cvng.beans.HostRecordDTO;
import io.harness.cvng.beans.LogRecordDTO;
import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventDTO;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;
import io.harness.rest.RestResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public interface CVNextGenServiceClient {
  @POST(DELEGATE_DATA_COLLECTION)
  Call<RestResponse<Boolean>> saveTimeSeriesMetrics(
      @Query("accountId") String accountId, @Body List<TimeSeriesDataCollectionRecord> metricData);

  @POST(LOG_RECORD_RESOURCE_PATH)
  Call<RestResponse<Void>> saveLogRecords(@Query("accountId") String accountId, @Body List<LogRecordDTO> logRecords);

  @POST(CVNG_LOG_RESOURCE_PATH)
  Call<RestResponse<Void>> saveCVNGLogRecords(
      @Query("accountId") String accountId, @Body List<CVNGLogDTO> cvngLogRecords);

  @POST(HOST_RECORD_RESOURCE_PATH)
  Call<RestResponse<Void>> saveHostRecords(@Query("accountId") String accountId, @Body List<HostRecordDTO> hostRecords);

  @GET(DELEGATE_DATA_COLLECTION_TASK + "/next-tasks")
  Call<RestResponse<List<DataCollectionTaskDTO>>> getNextDataCollectionTasks(
      @Query("accountId") String accountId, @Query("dataCollectionWorkerId") String dataCollectionWorkerId);

  @POST(DELEGATE_DATA_COLLECTION_TASK + "/update-status")
  Call<RestResponse<Void>> updateTaskStatus(
      @Query("accountId") String accountId, @Body DataCollectionTaskResult dataCollectionTaskResult);

  @POST(KUBERNETES_RESOURCE + "/activities")
  Call<RestResponse<Boolean>> saveKubernetesActivities(@Query("accountId") String accountId,
      @Query("activitySourceId") String activitySourceId, @Body List<KubernetesActivityDTO> activityDTOS);

  @POST(KUBERNETES_RESOURCE + "/change")
  Call<RestResponse<Boolean>> saveKubernetesChangeEvents(@Query("accountId") String accountId,
      @Query("changeSourceId") String changeSourceId, @Body ChangeEventDTO changeEventDTO);

  @GET(KUBERNETES_RESOURCE + "/source")
  Call<RestResponse<KubernetesActivitySourceDTO>> getKubernetesActivitySourceDTO(
      @Query("accountId") String accountId, @Query("dataCollectionWorkerId") String dataCollectionWorkerId);
}

package io.harness.verificationclient;

import static io.harness.cvng.core.services.CVNextGenConstants.DELEGATE_DATA_COLLECTION;
import static io.harness.cvng.core.services.CVNextGenConstants.DELEGATE_DATA_COLLECTION_TASK;

import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.core.services.entities.DataCollectionTask;
import io.harness.cvng.core.services.entities.DataCollectionTask.DataCollectionTaskResult;
import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

/**
 * Created by raghu on 09/17/18.
 */
public interface CVNextGenServiceClient {
  @POST(DELEGATE_DATA_COLLECTION)
  Call<RestResponse<Boolean>> saveTimeSeriesMetrics(
      @Query("accountId") String accountId, @Body List<TimeSeriesDataCollectionRecord> metricData);

  @GET(DELEGATE_DATA_COLLECTION_TASK + "/next-task")
  Call<RestResponse<DataCollectionTask>> getNextDataCollectionTask(
      @Query("accountId") String accountId, @Query("cvConfigId") String cvConfigId);

  @POST(DELEGATE_DATA_COLLECTION_TASK + "/update-status")
  Call<RestResponse<DataCollectionTask>> updateTaskStatus(
      @Query("accountId") String accountId, @Body DataCollectionTaskResult dataCollectionTaskResult);
}

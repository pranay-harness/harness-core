package io.harness.verificationclient;

import static io.harness.cvng.core.services.CVNextGenConstants.DELEGATE_DATA_COLLETION;

import io.harness.cvng.core.services.entities.TimeSeriesRecord;
import io.harness.rest.RestResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

import java.util.List;

/**
 * Created by raghu on 09/17/18.
 */
public interface CVNextGenServiceClient {
  @POST(DELEGATE_DATA_COLLETION + "/save-metrics")
  Call<RestResponse<Boolean>> saveTimeSeriesMetrics(@Query("accountId") String accountId,
      @Query("projectIdentifier") String stateExecutionId, @Body List<TimeSeriesRecord> metricData);
}

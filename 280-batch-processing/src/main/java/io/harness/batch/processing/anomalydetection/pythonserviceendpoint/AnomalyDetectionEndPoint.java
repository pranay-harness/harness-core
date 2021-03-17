package io.harness.batch.processing.anomalydetection.pythonserviceendpoint;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AnomalyDetectionEndPoint {
  @POST("/anomalydetection/v1") Call<List<PythonResponse>> prophet(@Body List<PythonInput> input);
}
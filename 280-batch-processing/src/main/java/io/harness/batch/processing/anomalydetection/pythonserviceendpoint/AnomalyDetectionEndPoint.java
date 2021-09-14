/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.anomalydetection.pythonserviceendpoint;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AnomalyDetectionEndPoint {
  @POST("/anomalydetection/v1") Call<List<PythonResponse>> prophet(@Body List<PythonInput> input);
}

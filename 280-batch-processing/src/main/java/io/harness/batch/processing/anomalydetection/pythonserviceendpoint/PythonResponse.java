/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.anomalydetection.pythonserviceendpoint;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class PythonResponse {
  String id;
  @SerializedName("anomaly_score") Double anomalyScore;
  Double y;
  @SerializedName("y_hat") Double yHat;
  @SerializedName("y_hat_lower") Double yHatLower;
  @SerializedName("y_hat_upper") Double yHatUpper;
  @SerializedName("is_anomaly") Boolean isAnomaly;
  Long timestamp;
}

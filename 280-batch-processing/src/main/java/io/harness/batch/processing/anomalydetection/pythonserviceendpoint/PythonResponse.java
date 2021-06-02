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

package io.harness.checks.buildpulse.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResult {
  @SerializedName("recorded_at") @Expose String recordedAt;
  @SerializedName("build_url") @Expose String buildUrl;
  @SerializedName("message") @Expose String message;
}

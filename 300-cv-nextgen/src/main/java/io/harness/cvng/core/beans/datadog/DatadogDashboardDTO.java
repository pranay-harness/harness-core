package io.harness.cvng.core.beans.datadog;

import lombok.Builder;
import lombok.Value;
import com.google.gson.annotations.SerializedName;
@Value
@Builder
public class DatadogDashboardDTO {
    String id;
    @SerializedName(value = "title")
    String name;
    @SerializedName(value = "url")
    String path;
}

package io.harness.cvng.core.beans.datadog;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DatadogDashboardDTO {
    String id;
    @SerializedName(value = "title")
    String name;
    @SerializedName(value = "url")
    String path;
}

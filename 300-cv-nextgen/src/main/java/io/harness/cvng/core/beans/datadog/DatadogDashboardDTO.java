package io.harness.cvng.core.beans.datadog;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DatadogDashboardDTO {
    String id;
    String title;
    String url;
}

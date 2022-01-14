package io.harness.cvng.core.beans;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class SampleDataDTO {
  @NotNull String jsonResponse;
  @NotNull String groupName;
  @NotNull String metricValueJSONPath;
  @NotNull String timestampJSONPath;
  String timestampFormat;
}

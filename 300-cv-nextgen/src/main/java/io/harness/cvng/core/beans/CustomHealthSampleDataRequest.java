package io.harness.cvng.core.beans;

import io.harness.delegate.beans.connector.customhealthconnector.CustomHealthMethod;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomHealthSampleDataRequest {
  @NotNull @NotBlank String urlPath;
  @NotNull Map<String, String> requestTimestampPlaceholderAndValues;
  @NotNull CustomHealthMethod method;
  String body;
}

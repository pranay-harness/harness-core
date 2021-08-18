package io.harness.cvng.core.beans.monitoredService;

import static io.harness.cvng.CVConstants.DATA_SOURCE_TYPE;

import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.ChangeSourceSpec;
import io.harness.cvng.core.types.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeSourceDTO {
  @NotEmpty String identifier;
  @NotEmpty String description;

  @JsonProperty(DATA_SOURCE_TYPE) ChangeSourceType type;
  boolean enabled;

  ChangeSourceSpec spec;
}

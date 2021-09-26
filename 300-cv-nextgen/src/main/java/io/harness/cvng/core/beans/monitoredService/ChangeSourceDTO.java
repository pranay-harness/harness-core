package io.harness.cvng.core.beans.monitoredService;

import static io.harness.cvng.CVConstants.DATA_SOURCE_TYPE;

import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.core.beans.monitoredService.changeSourceSpec.ChangeSourceSpec;
import io.harness.data.validator.EntityIdentifier;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ChangeSourceDTO {
  @NotEmpty String name;
  @NotEmpty @EntityIdentifier String identifier;
  @JsonProperty(DATA_SOURCE_TYPE) ChangeSourceType type;

  boolean enabled;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = DATA_SOURCE_TYPE, include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
      visible = true)
  @Valid
  @NotNull
  ChangeSourceSpec spec;

  public ChangeCategory getCategory() {
    return type.getChangeCategory();
  }
}

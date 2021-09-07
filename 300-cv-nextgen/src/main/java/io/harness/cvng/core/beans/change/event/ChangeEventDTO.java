package io.harness.cvng.core.beans.change.event;

import io.harness.cvng.core.beans.change.event.metadata.ChangeEventMetaData;
import io.harness.cvng.core.types.ChangeSourceType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class ChangeEventDTO {
  @NotNull String accountId;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;

  @NotNull String serviceIdentifier;
  @NotNull String envIdentifier;

  String changeSourceIdentifier;

  @JsonProperty("type") ChangeSourceType type;

  long eventTime;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXTERNAL_PROPERTY, visible = true)
  @Valid
  @NotNull
  ChangeEventMetaData changeEventMetaData;

  public ChangeSourceType getType() {
    if (type == null && changeEventMetaData != null) {
      type = changeEventMetaData.getType();
    }
    return type;
  }
}

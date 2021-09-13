package io.harness.cvng.beans.change;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@SuperBuilder
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
  ChangeEventMetadata changeEventMetaData;

  public ChangeSourceType getType() {
    if (type == null && changeEventMetaData != null) {
      type = changeEventMetaData.getType();
    }
    return type;
  }
}
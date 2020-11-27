package io.harness.cvng.cd10.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
@NoArgsConstructor
public abstract class CD10MappingDTO {
  private String appId;
  public abstract MappingType getType();
}

package io.harness.cvng.beans.activity;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY)
public abstract class ActivitySourceDTO {
  String uuid;
  @NotNull String identifier;
  @NotNull String name;
  long createdAt;
  long lastUpdatedAt;

  public abstract ActivitySourceType getType();
}

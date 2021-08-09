package io.harness.cvng.beans.activity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("INFRASTRUCTURE")
public class InfrastructureActivityDTO extends ActivityDTO {
  String message;

  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }
}

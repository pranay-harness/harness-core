package io.harness.cvng.beans.job;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("HEALTH")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class HealthVerificationJobDTO extends VerificationJobDTO {
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.HEALTH;
  }
}
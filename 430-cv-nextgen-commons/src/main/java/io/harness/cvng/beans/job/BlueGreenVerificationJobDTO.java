package io.harness.cvng.beans.job;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonTypeName("BLUE_GREEN")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BlueGreenVerificationJobDTO extends VerificationJobDTO {
  private String sensitivity;
  private String trafficSplitPercentage;

  @Override
  public VerificationJobType getType() {
    return VerificationJobType.BLUE_GREEN;
  }
}

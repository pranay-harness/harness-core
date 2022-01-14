package io.harness.cvng.beans.job;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@JsonTypeName("TEST")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TestVerificationJobDTO extends VerificationJobDTO {
  private String sensitivity;
  private String baselineVerificationJobInstanceId;
  @Override
  public VerificationJobType getType() {
    return VerificationJobType.TEST;
  }
}

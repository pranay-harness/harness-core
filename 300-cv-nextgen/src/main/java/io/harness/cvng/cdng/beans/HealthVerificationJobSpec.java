package io.harness.cvng.cdng.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@Data
@JsonTypeName("Health")
public class HealthVerificationJobSpec extends VerificationJobSpec {
  @Override
  public String getType() {
    return "Health";
  }
}

package io.harness.cvng.cdng.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@JsonTypeName("Health")
@SuperBuilder
@NoArgsConstructor
public class HealthVerificationJobSpec extends VerificationJobSpec {
  @Override
  public String getType() {
    return "Health";
  }

  @Override
  protected void addToRuntimeParams(HashMap<String, String> runtimeParams) {}
}

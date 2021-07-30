package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("NUMBER")
@OwnedBy(PL)
public class NumberAllowedValue implements AllowedValueYaml {
  Number number;

  public static final class Yaml { private Number number; }
}

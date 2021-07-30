package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("TEXT")
@OwnedBy(PL)
public class StringAllowedValue implements AllowedValueYaml {
  String value;

  public static final class Yaml { private String value; }
}

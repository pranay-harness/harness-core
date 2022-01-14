package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("NUMBER")
public class NumberAllowedValue implements AllowedValueYaml {
  Number number;

  public static final class Yaml { private Number number; }
}

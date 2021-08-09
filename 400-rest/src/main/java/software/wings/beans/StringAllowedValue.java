package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("TEXT")
public class StringAllowedValue implements AllowedValueYaml {
  String value;

  public static final class Yaml { private String value; }
}

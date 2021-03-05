package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NameValuePairYaml extends NameValuePairAbstractYaml {
  @Builder
  public NameValuePairYaml(String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamlList) {
    super(name, value, valueType, allowedValueYamlList);
  }
}

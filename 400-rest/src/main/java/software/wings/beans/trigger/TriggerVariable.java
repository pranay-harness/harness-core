package software.wings.beans.trigger;

import software.wings.beans.AllowedValueYaml;
import software.wings.beans.NameValuePairAbstractYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TriggerVariable extends NameValuePairAbstractYaml {
  String entityType;

  @Builder
  public TriggerVariable(
      String entityType, String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamls) {
    super(name, value, valueType, allowedValueYamls);
    this.entityType = entityType;
  }
}

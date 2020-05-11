package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.beans.AllowedValueYaml;
import software.wings.beans.NameValuePair;

import java.util.List;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TriggerVariableYaml extends NameValuePair.AbstractYaml {
  String entityType;

  @Builder
  public TriggerVariableYaml(
      String entityType, String name, String value, String valueType, List<AllowedValueYaml> allowedValueYamls) {
    super(name, value, valueType, allowedValueYamls);
    this.entityType = entityType;
  }
}

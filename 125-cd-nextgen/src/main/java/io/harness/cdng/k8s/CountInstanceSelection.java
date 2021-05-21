package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.integer;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("Count")
public class CountInstanceSelection implements InstanceSelectionBase {
  @YamlSchemaTypes({string, integer}) ParameterField<Integer> count;
  @Override
  public K8sInstanceUnitType getType() {
    return K8sInstanceUnitType.Count;
  }

  @Override
  public Integer getInstances() {
    if (ParameterField.isNull(this.count)) {
      return null;
    }

    return count.getValue();
  }
}

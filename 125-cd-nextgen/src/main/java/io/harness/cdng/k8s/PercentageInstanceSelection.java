package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.number;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("Percentage")
public class PercentageInstanceSelection implements InstanceSelectionBase {
  @YamlSchemaTypes({string, number}) ParameterField<Integer> percentage;
  @Override
  public K8sInstanceUnitType getType() {
    return K8sInstanceUnitType.Percentage;
  }

  @Override
  public Integer getInstances() {
    if (ParameterField.isNull(percentage)) {
      return null;
    }

    return percentage.getValue();
  }
}

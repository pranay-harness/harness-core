package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.number;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("Percentage")
public class PercentageInstanceSelection implements InstanceSelectionBase {
  @YamlSchemaTypes({string, number}) ParameterField<String> percentage;
  @Override
  public K8sInstanceUnitType getType() {
    return K8sInstanceUnitType.Percentage;
  }

  @Override
  public Integer getInstances() {
    if (ParameterField.isNull(percentage)) {
      return null;
    }
    try {
      return new BigDecimal(percentage.getValue()).intValueExact();
    } catch (Exception exception) {
      throw new InvalidRequestException(
          String.format("Percentage value: [%s] is not an integer", percentage.getValue()), exception);
    }
  }
}

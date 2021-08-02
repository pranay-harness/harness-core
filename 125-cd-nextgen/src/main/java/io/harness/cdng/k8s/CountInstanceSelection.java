package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.integer;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("Count")
public class CountInstanceSelection implements InstanceSelectionBase {
  @YamlSchemaTypes({string, integer}) ParameterField<String> count;
  @Override
  public K8sInstanceUnitType getType() {
    return K8sInstanceUnitType.Count;
  }

  @Override
  public Integer getInstances() {
    if (ParameterField.isNull(this.count)) {
      return null;
    }
    try {
      return new BigDecimal(count.getValue()).intValueExact();
    } catch (Exception exception) {
      throw new InvalidRequestException(
          String.format("Count value: [%s] is not an integer", count.getValue()), exception);
    }
  }
}

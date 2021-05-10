package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("Count")
public class CountInstanceSelection implements InstanceSelectionBase {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<Integer> count;
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

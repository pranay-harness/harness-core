package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@JsonTypeName("Count")
public class CountInstanceSelection implements InstanceSelectionBase {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> count;
  @Override
  public K8sInstanceUnitType getType() {
    return K8sInstanceUnitType.Count;
  }
}

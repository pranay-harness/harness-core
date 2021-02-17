package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@JsonTypeName("ReleaseName")
public class DeleteReleaseNameSpec implements DeleteResourcesBaseSpec {
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> deleteNamespace;
  @Override
  public DeleteResourcesType getType() {
    return DeleteResourcesType.ReleaseName;
  }

  @Override
  public String getResources() {
    return StringUtils.EMPTY;
  }

  @Override
  public Boolean getDeleteNamespace() {
    return deleteNamespace != null && deleteNamespace.getValue() != null && deleteNamespace.getValue();
  }

  @Override
  public Boolean getAllManifestPaths() {
    return Boolean.FALSE;
  }
}

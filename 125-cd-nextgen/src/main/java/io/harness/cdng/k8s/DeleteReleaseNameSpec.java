package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("ReleaseName")
public class DeleteReleaseNameSpec implements DeleteResourcesBaseSpec {
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> deleteNamespace;

  @Override
  public DeleteResourcesType getType() {
    return DeleteResourcesType.ReleaseName;
  }

  @Override
  public String getResourceNamesValue() {
    return "";
  }

  @Override
  public String getManifestPathsValue() {
    return "";
  }

  @Override
  public Boolean getDeleteNamespaceValue() {
    return deleteNamespace != null && deleteNamespace.getValue() != null && deleteNamespace.getValue();
  }

  @Override
  public Boolean getAllManifestPathsValue() {
    return Boolean.FALSE;
  }
}

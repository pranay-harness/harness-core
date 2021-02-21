package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
@JsonTypeName("ResourceName")
public class DeleteResourceNameSpec implements DeleteResourcesBaseSpec {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> resourceNames;
  @Override
  public DeleteResourcesType getType() {
    return DeleteResourcesType.ResourceName;
  }

  @Override
  public String getResourceNames() {
    List<String> resourceNamesList = resourceNames != null ? resourceNames.getValue() : Collections.emptyList();
    String resourceNames = resourceNamesList.stream().collect(Collectors.joining(","));
    return resourceNames;
  }

  @Override
  public String getManifestPaths() {
    return "";
  }

  @Override
  public Boolean getDeleteNamespace() {
    return Boolean.FALSE;
  }

  @Override
  public Boolean getAllManifestPaths() {
    return Boolean.FALSE;
  }
}

package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@Data
@JsonTypeName("ManifestPath")
public class DeleteManifestPathSpec implements DeleteResourcesBaseSpec {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> manifestPaths;
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> allManifestPaths;

  @Override
  public DeleteResourcesType getType() {
    return DeleteResourcesType.ManifestPath;
  }

  @Override
  public String getManifestPaths() {
    List<String> filePathsList = manifestPaths != null ? manifestPaths.getValue() : Collections.emptyList();
    return filePathsList.stream().collect(Collectors.joining(","));
  }

  @Override
  public String getResourceNames() {
    return "";
  }

  @Override
  public Boolean getDeleteNamespace() {
    return Boolean.FALSE;
  }

  @Override
  public Boolean getAllManifestPaths() {
    return allManifestPaths != null && allManifestPaths.getValue() != null && allManifestPaths.getValue();
  }
}

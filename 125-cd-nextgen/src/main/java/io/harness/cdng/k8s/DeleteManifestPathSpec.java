package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@OwnedBy(CDP)
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
  public String getManifestPathsValue() {
    List<String> filePathsList = manifestPaths != null ? manifestPaths.getValue() : Collections.emptyList();
    return filePathsList.stream().collect(Collectors.joining(","));
  }

  @Override
  public String getResourceNamesValue() {
    return "";
  }

  @Override
  public Boolean getAllManifestPathsValue() {
    return allManifestPaths != null && allManifestPaths.getValue() != null && allManifestPaths.getValue();
  }
}

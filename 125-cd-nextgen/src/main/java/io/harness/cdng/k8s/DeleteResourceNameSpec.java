package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
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
@JsonTypeName("ResourceName")
public class DeleteResourceNameSpec implements DeleteResourcesBaseSpec {
  @ApiModelProperty(allowableValues = "ResourceName")
  private DeleteResourcesType type = DeleteResourcesType.ResourceName;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> resourceNames;
  @Override
  public DeleteResourcesType getType() {
    return type;
  }

  @Override
  public String getResourceNames() {
    List<String> resourceNamesList = resourceNames != null ? resourceNames.getValue() : Collections.emptyList();
    return resourceNamesList.stream().collect(Collectors.joining(","));
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

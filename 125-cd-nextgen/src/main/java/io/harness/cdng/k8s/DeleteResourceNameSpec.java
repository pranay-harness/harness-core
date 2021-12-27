package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

@OwnedBy(CDP)
@Data
@JsonTypeName("ResourceName")
@RecasterAlias("io.harness.cdng.k8s.DeleteResourceNameSpec")
public class DeleteResourceNameSpec implements DeleteResourcesBaseSpec {
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @NotNull @NotEmpty ParameterField<List<String>> resourceNames;

  @Override
  public DeleteResourcesType getType() {
    return DeleteResourcesType.ResourceName;
  }

  @Override
  public String getResourceNamesValue() {
    List<String> resourceNamesList = resourceNames != null && resourceNames.getValue() != null ?
            resourceNames.getValue() : Collections.emptyList();
    return resourceNamesList.stream().collect(Collectors.joining(","));
  }

  @Override
  public String getManifestPathsValue() {
    return "";
  }

  @Override
  public Boolean getAllManifestPathsValue() {
    return Boolean.FALSE;
  }
}
package io.harness.yaml.extended.ci.codebase;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.yaml.extended.ci.CodeBase")
public class CodeBase {
  @NotNull String connectorRef;
  String repoName;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = "io.harness.yaml.extended.ci.codebase.Build")
  @NotNull
  ParameterField<Build> build;
}

package io.harness.yaml.extended.ci.codebase;

import io.harness.pms.yaml.ParameterField;

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
  @ApiModelProperty(dataType = "io.harness.yaml.extended.ci.codebase.Build") @NotNull ParameterField<Build> build;
}
